package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.User;
import br.com.flowlinkerAPI.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import org.slf4j.*;
import br.com.flowlinkerAPI.dto.CreatedUserResultDTO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.repository.DeviceRepository;
import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import br.com.flowlinkerAPI.exceptions.LimitDevicesException;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import br.com.flowlinkerAPI.exceptions.CustomerNotFoundException;

@Service
public class UserService {

    
    private final UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private CustomerRepository customerRepository;
    
    @Value("${jwt.secret}")
    private String jwtSecret;

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

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

    public String loginAndGenerateToken(String username, String password, String type, String fingerprint, HttpServletResponse response) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        long expirationMillis = "device".equals(type) ? 604800000L : 86400000L;     
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", type);
        
        String redisKey;
        
        if ("device".equals(type)) {
            if (fingerprint == null || fingerprint.isEmpty()) {
                throw new RuntimeException("Fingerprint required for device authentication");
            }
            claims.put("fingerprint", fingerprint);
            
            Device device = deviceRepository.findByFingerprint(fingerprint)
                .orElseGet(() -> {
                    Customer customer = customerRepository.findById(user.getCustomer().getId())
                        .orElseThrow(() -> new CustomerNotFoundException("Customer not found for user " + username));
                    
                    int currentCount = deviceRepository.countByCustomerId(customer.getId());
                    int max = getMaxDevices(customer.getOfferType());  
                    
                    if (currentCount >= max) {
                        throw new LimitDevicesException("Device limit reached");
                    }
                    
                    Device newDevice = new Device();
                    newDevice.setFingerprint(fingerprint);
                    newDevice.setName("Auto-generated device");
                    newDevice.setCustomer(customer);
                    return deviceRepository.save(newDevice);
                });
            redisKey = "device:token:" + fingerprint;
        } else {
            redisKey = type + ":token:" + username;
        }
        
        String token = Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
        
            if("web".equals(type)) {
                Cookie cookie = new Cookie("jwtToken", token);
                cookie.setHttpOnly(true);
                cookie.setSecure(true);
                cookie.setMaxAge(86400);
                cookie.setPath("/");
                response.addCookie(cookie);
                
                redisTemplate.opsForValue().set(redisKey, token, Duration.ofMillis(expirationMillis));

                return null;

            }

            redisTemplate.opsForValue().set(redisKey, token, Duration.ofMillis(expirationMillis));
            return token;
        
    }

    private int getMaxDevices(Customer.OfferType offerType) {
        Map<Customer.OfferType, Integer> maxDevices = new HashMap<>();
        maxDevices.put(Customer.OfferType.BASIC, 3);
        maxDevices.put(Customer.OfferType.STANDARD, 5);
        maxDevices.put(Customer.OfferType.PRO, 10);
        return maxDevices.getOrDefault(offerType, 0);
    }
}
