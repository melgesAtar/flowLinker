package br.com.flowlinkerAPI.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import br.com.flowlinkerAPI.repository.UserRepository;
import br.com.flowlinkerAPI.model.User;
import br.com.flowlinkerAPI.service.email.EmailService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class PasswordResetService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RedisTemplate<String, String> redisTemplate;
	private final EmailService emailService;

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${password.reset.ttl.minutes:15}")
	private long resetTtlMinutes;

	@Value("${password.reset.redirect-base-url}")
	private String defaultRedirectBaseUrl;

	public PasswordResetService(UserRepository userRepository,
	                            PasswordEncoder passwordEncoder,
	                            @Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate,
	                            EmailService emailService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.redisTemplate = redisTemplate;
		this.emailService = emailService;
	}

	public void sendResetEmail(String email, String redirectBaseUrl) {
		User user = userRepository.findByUsername(email).orElse(null);
		
		if (user == null) return;

		long expirationMs = Duration.ofMinutes(resetTtlMinutes).toMillis();
		String jti = UUID.randomUUID().toString();

		SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

		String token = Jwts.builder()
			.claim("type", "password_reset")
			.id(jti)
			.subject(email)
			.issuedAt(new Date())
			.expiration(new Date(System.currentTimeMillis() + expirationMs))
			.signWith(key, Jwts.SIG.HS512)
			.compact();

		// Guarda o token no Redis (chave por token) para validar por lá também
		String redisKey = "pwdreset:token:" + token;
		redisTemplate.opsForValue().set(redisKey, email, Duration.ofMillis(expirationMs));

		String base = (redirectBaseUrl != null && !redirectBaseUrl.isBlank())
			? redirectBaseUrl
			: defaultRedirectBaseUrl;
		String link = base + "?token=" + token;

		emailService.sendPasswordResetEmail(email, link);
	}

	@Transactional
	public void resetPassword(String token, String newPassword) {
		if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
			throw new IllegalArgumentException("Token e nova senha são obrigatórios");
		}
		SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		Claims claims = Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();

		if (!"password_reset".equals(String.valueOf(claims.get("type")))) {
			throw new IllegalArgumentException("Token inválido");
		}
		String username = claims.getSubject();
		String redisKey = "pwdreset:token:" + token;
		String value = redisTemplate.opsForValue().get(redisKey);
		if (value == null || !value.equals(username)) {
			throw new IllegalArgumentException("Token expirado ou inválido");
		}

		User user = userRepository.findByUsername(username)
			.orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);

		redisTemplate.delete(redisKey);
	}
}


