package br.com.flowlinkerAPI.config.filter;

import br.com.flowlinkerAPI.service.CorsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CustomCorsFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CustomCorsFilter.class);
    private final CorsService corsService;

    public CustomCorsFilter(CorsService corsService) {
        this.corsService = corsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        boolean hasOrigin = origin != null && !origin.isBlank();
        String acrMethod = request.getHeader("Access-Control-Request-Method");

        if (hasOrigin && corsService.isAllowedOrigin(origin)) {
            corsService.applyCorsHeaders(request, response, origin);
            // Preflight: responde imediatamente
            if ("OPTIONS".equalsIgnoreCase(request.getMethod()) && acrMethod != null) {
                response.setStatus(HttpServletResponse.SC_OK);
                logger.debug("CORS preflight OK origin={} method={} path={}", origin, request.getMethod(), request.getRequestURI());
                return;
            }
        } else if (hasOrigin) {

            logger.warn("CORS origin not allowed origin={} path={} method={}", origin, request.getRequestURI(), request.getMethod());
        }

        filterChain.doFilter(request, response);
    }
}


