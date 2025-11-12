package br.com.flowlinkerAPI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CorsService {

    private static final Logger logger = LoggerFactory.getLogger(CorsService.class);

    private final List<Pattern> allowedOriginPatterns;
    private final String allowedMethods;
    private final String allowedHeaders;
    private final String exposedHeaders;
    private final boolean allowCredentials;

    public CorsService(
            @Value("${cors.allowedOriginPatterns:https://*.ngrok-free.app,https://*.ngrok.app,http://localhost:*,https://localhost:*,http://127.0.0.1:*}") String originPatterns,
            @Value("${cors.allowedMethods:GET,POST,PUT,DELETE,OPTIONS}") String allowedMethods,
            @Value("${cors.allowedHeaders:*}") String allowedHeaders,
            @Value("${cors.exposedHeaders:Authorization}") String exposedHeaders,
            @Value("${cors.allowCredentials:true}") boolean allowCredentials
    ) {
        var rawList = java.util.Arrays.stream(originPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        this.allowedOriginPatterns = rawList.stream()
                .map(CorsService::wildcardToRegex)
                .map(Pattern::compile)
                .collect(Collectors.toList());
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.exposedHeaders = exposedHeaders;
        this.allowCredentials = allowCredentials;

        // Loga configuração efetiva
        logger.info("[CORS] allowedOriginPatterns(raw)   = {}", String.join(", ", rawList));
        logger.info("[CORS] allowedOriginPatterns(regex) = {}", this.allowedOriginPatterns.stream().map(Pattern::pattern).collect(Collectors.joining(", ")));
        logger.info("[CORS] allowCredentials={} allowMethods={} allowHeaders={} exposedHeaders={}", allowCredentials, allowedMethods, allowedHeaders, exposedHeaders);
    }

    public boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) return false;
        for (Pattern p : allowedOriginPatterns) {
            if (p.matcher(origin).matches()) return true;
        }
        return false;
    }

    public void applyCorsHeaders(jakarta.servlet.http.HttpServletRequest request,
                                 jakarta.servlet.http.HttpServletResponse response,
                                 String origin) {
        response.setHeader("Access-Control-Allow-Origin", origin);
        if (allowCredentials) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", allowedMethods);
        response.setHeader("Access-Control-Allow-Headers", allowedHeaders);
        response.setHeader("Access-Control-Expose-Headers", exposedHeaders);
        response.addHeader("Vary", "Origin");
        response.addHeader("Vary", "Access-Control-Request-Method");
        response.addHeader("Vary", "Access-Control-Request-Headers");
    }

    private static String wildcardToRegex(String pattern) {
        String escaped = Pattern.quote(pattern);
        // converte '*' para '.*' mantendo o restante escapado
        return "^" + escaped.replace("\\*", ".*") + "$";
    }
}


