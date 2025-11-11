package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.User;
import br.com.flowlinkerAPI.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import org.slf4j.*;
import br.com.flowlinkerAPI.dto.CreatedUserResultDTO;
import io.jsonwebtoken.Jwts;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.model.DeviceStatus;
import br.com.flowlinkerAPI.repository.DeviceRepository;
import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import br.com.flowlinkerAPI.exceptions.LimitDevicesException;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import br.com.flowlinkerAPI.exceptions.CustomerNotFoundException;
import br.com.flowlinkerAPI.exceptions.BadCredentialsException;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import br.com.flowlinkerAPI.exceptions.DeviceChangedException;

@Service
public class UserService {

    
    private final UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    @Qualifier("stringRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private HardwareFingerPrintService hardwareFingerPrintService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private DevicePolicyService devicePolicyService;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createOrGetUserByEmail(String email) {

        return userRepository.findByUsername(email)
        .orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(email);
            String randomPassword = generateRandomPassword(12);
            String hashedPassword = passwordEncoder.encode(randomPassword);
            newUser.setPassword(hashedPassword);
            return userRepository.save(newUser);
        });
    }

    public CreatedUserResultDTO createOrGetUserWithPasswordReturn(String email) {
        return userRepository.findByUsername(email)
            .map(u -> new CreatedUserResultDTO(u, false, null))
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername(email);
                String randomPassword = generateRandomPassword(12);
                String hashedPassword = passwordEncoder.encode(randomPassword);
                newUser.setPassword(hashedPassword);
                User saved = userRepository.save(newUser);
                return new CreatedUserResultDTO(saved, true, randomPassword);
            });
    }


    public String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public String loginAndGenerateToken(String username, String password, String type, String fingerprint, String deviceId, String hwHash,
                                        String osName, String osVersion, String arch, String hostname, String appVersion,
                                        HttpServletRequest request, HttpServletResponse response) {
       
        User user = userRepository.findByUsername(username)
             .orElseThrow(() -> new BadCredentialsException("Invalid Credentials"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid Credentials");
        }
        logger.info("Login bem-sucedido username={} type={} customerId={}", username, type, (user.getCustomer() != null ? String.valueOf(user.getCustomer().getId()) : null));
        
        long expirationMillis = "device".equals(type) ? 604800000L : 86400000L;     
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", type);
        
        String redisKey;
        
        if ("device".equals(type)) {
            logger.info("Fluxo device authentication username={} deviceId={} fingerprint={}", username, deviceId, fingerprint);
            if (fingerprint == null || fingerprint.isEmpty())
                throw new IllegalArgumentException("Fingerprint required for device authentication");
    
            Long customerId = user.getCustomer().getId();
            claims.put("type", type);
            claims.put("fingerprint", fingerprint);
            claims.put("customerId", customerId);
    
            Device device = null;
    
            if (deviceId != null && !deviceId.isEmpty()) {
                device = deviceRepository.findByCustomerIdAndDeviceId(customerId, deviceId).orElse(null);
            }
            if (device == null) {
                device = deviceRepository.findByCustomerIdAndFingerprint(customerId, fingerprint).orElse(null);
            }
    
            if (device == null) {
                Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException("Customer not found for user " + username));
    
                int currentCount = deviceRepository.countByCustomerIdAndStatus(customerId, DeviceStatus.ACTIVE);
                int max = devicePolicyService.getAllowedDevices(customerId, customer.getOfferType());
                if (currentCount >= max) throw new LimitDevicesException("Sem máquinas disponíveis. Revogue o acesso de uma máquina no painel administrativo para liberar uma vaga.");
    
                Device newDevice = new Device();
                newDevice.setCustomer(customer);
                newDevice.setStatus(DeviceStatus.ACTIVE);
                newDevice.setName("Auto-generated device");
    
                newDevice.setDeviceId(deviceId);           
                newDevice.setFingerprint(fingerprint);     
                newDevice.setHwHashBaseline(hwHash);       
                newDevice.setLastHwHash(hwHash);
                newDevice.setOsName(osName);
                newDevice.setOsVersion(osVersion);
                newDevice.setArch(arch);
                newDevice.setHostname(hostname);
                newDevice.setAppVersion(appVersion);
                newDevice.setLastIp(extractClientIp(request));
                newDevice.setLastSeenAt(java.time.Instant.now());
                device = deviceRepository.save(newDevice);
                logger.info("Novo device cadastrado customerId={} deviceId={} fingerprint={} os={} {} app={} ip={}",
                    String.valueOf(customerId), deviceId, fingerprint, osName, osVersion, appVersion, newDevice.getLastIp());
            } else {
                boolean wasInactive = device.getStatus() == DeviceStatus.INACTIVE;
                if (wasInactive){
                    int active = deviceRepository.countByCustomerIdAndStatus(customerId, DeviceStatus.ACTIVE);
                    int max = devicePolicyService.getAllowedDevices(customerId, user.getCustomer().getOfferType());
                    if(active >= max){
                        throw new LimitDevicesException("Sem máquinas disponíveis. Revogue o acesso de uma máquina no painel administrativo para liberar uma vaga.");
                    }
                    device.setStatus(DeviceStatus.ACTIVE);
                }
                if (hwHash != null && !hwHash.isEmpty()) {
                    device.setLastHwHash(hwHash);
                    // Se o dispositivo foi reativado após revogação, recomeça a baseline
                    if (device.getHwHashBaseline() == null || wasInactive) {
                        device.setHwHashBaseline(hwHash);
                    } else {
                        double diff = hardwareFingerPrintService.diffRatio(device.getHwHashBaseline(), hwHash);
                        if(diff > 0.7) {
                            throw new DeviceChangedException("Device changed");
                        }
                    }
                }
                if (deviceId != null && !deviceId.isEmpty() && device.getDeviceId() == null)
                    device.setDeviceId(deviceId);
                if (osName != null && !osName.isEmpty()) device.setOsName(osName);
                if (osVersion != null && !osVersion.isEmpty()) device.setOsVersion(osVersion);
                if (arch != null && !arch.isEmpty()) device.setArch(arch);
                if (hostname != null && !hostname.isEmpty()) device.setHostname(hostname);
                if (appVersion != null && !appVersion.isEmpty()) device.setAppVersion(appVersion);
                device.setLastIp(extractClientIp(request));
    
                device.setLastSeenAt(java.time.Instant.now());
                deviceRepository.save(device);
                logger.info("Device atualizado customerId={} deviceId={} fingerprint={} os={} {} app={} ip={} status={}",
                    String.valueOf(customerId), device.getDeviceId(), device.getFingerprint(), device.getOsName(), device.getOsVersion(), device.getAppVersion(), device.getLastIp(), device.getStatus());
            }
    
            redisKey = "device:token:" + customerId + ":" + fingerprint;
        } else {
            // incluir customerId também para web
            if (user.getCustomer() != null && user.getCustomer().getId() != null) {
                claims.put("customerId", user.getCustomer().getId());
            }
            redisKey = type + ":token:" + username;
            logger.info("Fluxo web authentication username={} customerId={}", username, (user.getCustomer() != null ? String.valueOf(user.getCustomer().getId()) : null));
        }
            
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMillis))
            .signWith(key, Jwts.SIG.HS512) 
            .compact();
                
            if("web".equals(type)) {
                ResponseCookie cookie = ResponseCookie.from("jwtToken", token)
                    .httpOnly(true)
                    .secure(false)          // DEV: false; PROD: true
                    .sameSite("Lax")        
                    .path("/")
                    .maxAge(Duration.ofDays(1))
                    .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                
                redisTemplate.opsForValue().set(redisKey, token, Duration.ofMillis(expirationMillis));
                logger.info("Token web emitido username={} expMs={}", username, expirationMillis);
                return null;

            }

            redisTemplate.opsForValue().set(redisKey, token, Duration.ofMillis(expirationMillis));
            logger.info("Token device emitido username={} fingerprint={} expMs={}", username, fingerprint, expirationMillis);
            return token;
        
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            int comma = xf.indexOf(',');
            return comma > 0 ? xf.substring(0, comma).trim() : xf.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) return realIp.trim();
        return request.getRemoteAddr();
    }

    
}
