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
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import br.com.flowlinkerAPI.config.filter.RequestLoggingFilter;
import br.com.flowlinkerAPI.config.filter.CorsAuditFilter;
import br.com.flowlinkerAPI.config.security.CurrentRequest;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public CorsAuditFilter corsAuditFilter() {
        return new CorsAuditFilter();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AuthenticationManager authenticationManager,
                                                        @Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate,
                                                        @Value("${jwt.secret}") String jwtSecret) {
        return new JwtAuthenticationFilter(authenticationManager, redisTemplate, jwtSecret);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager, JwtAuthenticationFilter jwtAuthenticationFilter, RequestLoggingFilter requestLoggingFilter, CurrentRequest currentRequest, CorsAuditFilter corsAuditFilter) throws Exception {
        http
        .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
            .requestMatchers(HttpMethod.OPTIONS).permitAll()
            .requestMatchers("/stripe/**").permitAll()
            .requestMatchers("/auth/login").permitAll()
            .requestMatchers("/auth/password/**").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
            .requestMatchers("/admin/releases/quick/**").permitAll()
            .requestMatchers("/devices/limits").permitAll()
            .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(requestLoggingFilter, JwtAuthenticationFilter.class)
            .addFilterBefore(corsAuditFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(new InactiveDeviceFilter(currentRequest), JwtAuthenticationFilter.class);  
        return http.build();
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
    public CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowedOriginPatterns:https://*.ngrok-free.app,https://*.ngrok.app,http://localhost:*,https://localhost:*,http://127.0.0.1:*}") String allowedOriginsCsv){
        CorsConfiguration configuration = new CorsConfiguration();
        // Permite origens dinâmicas (ex.: ngrok) e ambientes locais - parametrizável por env/properties
        List<String> patterns = java.util.Arrays.stream(allowedOriginsCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Libera todos os headers para não bloquear preflight (Access-Control-Request-Headers)
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;

    }
}
