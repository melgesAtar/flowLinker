package br.com.flowlinkerAPI.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CorsAuditFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CorsAuditFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        String acrMethod = request.getHeader("Access-Control-Request-Method");
        String acrHeaders = request.getHeader("Access-Control-Request-Headers");
        String referer = request.getHeader("Referer");
        String xfwdFor = request.getHeader("X-Forwarded-For");
        String ip = (xfwdFor != null && !xfwdFor.isBlank())
                ? xfwdFor.split(",")[0].trim()
                : request.getRemoteAddr();

        StatusCaptureHttpServletResponse wrapped = new StatusCaptureHttpServletResponse(response);
        filterChain.doFilter(request, wrapped);

        // Loga apenas requisições que envolvem CORS (têm Origin) ou preflight (OPTIONS com ACR*)
        boolean isCorsRelated = origin != null || "OPTIONS".equalsIgnoreCase(request.getMethod());
        if (!isCorsRelated) return;

        int status = wrapped.getStatus();
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (status == 403) {
            logger.warn("CORS BLOCKED ip={} method={} path={} origin={} acrMethod={} acrHeaders={} referer={}",
                    ip, method, path, origin, acrMethod, acrHeaders, referer);
        } else if (status >= 400) {
            logger.info("CORS RELATED ERROR ip={} method={} path={} status={} origin={} acrMethod={} acrHeaders={}",
                    ip, method, path, status, origin, acrMethod, acrHeaders);
        } else {
            logger.debug("CORS OK ip={} method={} path={} status={} origin={} acrMethod={} acrHeaders={}",
                    ip, method, path, status, origin, acrMethod, acrHeaders);
        }
    }

    // Wrapper simples para capturar o status depois do chain
    private static class StatusCaptureHttpServletResponse extends jakarta.servlet.http.HttpServletResponseWrapper {
        private int httpStatus = 200;
        public StatusCaptureHttpServletResponse(HttpServletResponse response) { super(response); }
        @Override public void setStatus(int sc) { super.setStatus(sc); this.httpStatus = sc; }
        @Override public void sendError(int sc) throws IOException { super.sendError(sc); this.httpStatus = sc; }
        @Override public void sendError(int sc, String msg) throws IOException { super.sendError(sc, msg); this.httpStatus = sc; }
        @Override public void sendRedirect(String location) throws IOException { super.sendRedirect(location); this.httpStatus = 302; }
        public int getStatus() { return this.httpStatus; }
    }
}


