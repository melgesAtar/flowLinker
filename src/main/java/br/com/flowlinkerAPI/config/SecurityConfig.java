package br.com.flowlinkerAPI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import br.com.flowlinkerAPI.config.filter.JwtAuthenticationFilter;
import br.com.flowlinkerAPI.config.filter.InactiveDeviceFilter;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.beans.factory.annotation.Value;
import br.com.flowlinkerAPI.config.filter.RequestLoggingFilter;
import br.com.flowlinkerAPI.config.security.CurrentRequest;
import org.springframework.web.cors.CorsConfigurationSource;
import br.com.flowlinkerAPI.config.filter.ActiveSubscriptionFilter;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays; 
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AuthenticationManager authenticationManager,
                                                        @Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate,
                                                        @Value("${jwt.secret}") String jwtSecret) {
        return new JwtAuthenticationFilter(authenticationManager, redisTemplate, jwtSecret);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager, JwtAuthenticationFilter jwtAuthenticationFilter, RequestLoggingFilter requestLoggingFilter, CurrentRequest currentRequest, CustomerRepository customerRepository) throws Exception {
        http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
            .requestMatchers("/stripe/**").permitAll()
            .requestMatchers("/auth/login").permitAll()
            .requestMatchers("/auth/password/**").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
            .requestMatchers("/admin/releases/quick/**").permitAll()
            .requestMatchers("/devices/limits").permitAll()
            .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(requestLoggingFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(new InactiveDeviceFilter(currentRequest), JwtAuthenticationFilter.class)
            .addFilterAfter(new ActiveSubscriptionFilter(currentRequest, customerRepository), InactiveDeviceFilter.class);  
        return http
        .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://flowlinker.com.br, https://app.flowlinker.com.br,https://*.ngrok-free.app,http://localhost:3000,http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // Métodos permitidos
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
