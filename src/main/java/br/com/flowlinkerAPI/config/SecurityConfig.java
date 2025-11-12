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
import br.com.flowlinkerAPI.config.security.CurrentRequest;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AuthenticationManager authenticationManager,
                                                        @Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate,
                                                        @Value("${jwt.secret}") String jwtSecret) {
        return new JwtAuthenticationFilter(authenticationManager, redisTemplate, jwtSecret);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager, JwtAuthenticationFilter jwtAuthenticationFilter, RequestLoggingFilter requestLoggingFilter, CurrentRequest currentRequest) throws Exception {
        http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> {})
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
    public CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowedOriginPatterns}") String allowedOriginsCsv){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 1) Config restrita (com credenciais) para endpoints web que usam cookie (ex.: /auth/**)
        var originsInput = java.util.Arrays.stream(allowedOriginsCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
        // Separa exatos (sem wildcard) de padrões (com *)
        var exactOrigins = new java.util.ArrayList<String>();
        var patternOrigins = new java.util.ArrayList<String>();
        for (String o : originsInput) {
            if (o.contains("*")) patternOrigins.add(o);
            else exactOrigins.add(o);
        }
        CorsConfiguration webWithCookie = new CorsConfiguration();
        if (!exactOrigins.isEmpty()) {
            webWithCookie.setAllowedOrigins(exactOrigins);
        }
        if (!patternOrigins.isEmpty()) {
            webWithCookie.setAllowedOriginPatterns(patternOrigins);
        }
        webWithCookie.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        webWithCookie.setAllowedHeaders(List.of("*"));
        webWithCookie.setExposedHeaders(List.of("Authorization"));
        webWithCookie.setAllowCredentials(true);
        source.registerCorsConfiguration("/auth/**", webWithCookie);

        // 2) Config aberta (sem credenciais) para todos os demais endpoints (dispositivos ao redor do mundo)
        CorsConfiguration open = new CorsConfiguration();
        open.setAllowedOriginPatterns(List.of("*"));           // qualquer origem
        open.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        open.setAllowedHeaders(List.of("*"));
        open.setAllowCredentials(false);                       // obrigatório para aceitar '*'
        open.setExposedHeaders(List.of("Authorization"));
        source.registerCorsConfiguration("/**", open);

        return source;
    }
}
