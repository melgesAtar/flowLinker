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
                                                        RedisTemplate<String, String> redisTemplate,
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
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4020", "http://127.0.0.1:4020", "https://localhost:4200", "http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-Fingerprint", "X-Auth-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;

    }
}
