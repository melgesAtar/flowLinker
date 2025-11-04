package br.com.flowlinkerAPI.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import br.com.flowlinkerAPI.config.security.CurrentUser;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
			throws ServletException, IOException {

		long start = System.currentTimeMillis();
		String requestId = UUID.randomUUID().toString();
		MDC.put("requestId", requestId);

		String clientType = headerOrDefault(request, "X-Auth-Type", "web");
		String deviceId = headerOrDefault(request, "X-Device-Id", headerOrDefault(request, "X-Fingerprint", null));
		String clientIp = extractClientIp(request);
		MDC.put("clientType", clientType);
		if (deviceId != null) MDC.put("deviceId", deviceId);
		if (clientIp != null) MDC.put("clientIp", clientIp);

		populateSecurityContextMdc();

		String method = request.getMethod();
		String path = request.getRequestURI();
		String query = request.getQueryString();
		String ua = headerOrDefault(request, "User-Agent", "-");

		logger.info("REQ START {} {}{} ua={}", method, path, (query != null ? "?" + query : ""), ua);

		try {
			filterChain.doFilter(request, response);
		} finally {
			long dur = System.currentTimeMillis() - start;
			int status = response.getStatus();
			logger.info("REQ END {} {} status={} durMs={}", method, path, status, dur);
			MDC.clear();
		}
	}

	private void populateSecurityContextMdc() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated()) {
			Object principal = auth.getPrincipal();
			if (principal instanceof CurrentUser cu) {
				MDC.put("user", cu.username());
				if (cu.customerId() != null) {
					MDC.put("customerId", String.valueOf(cu.customerId()));
				}
			} else if (principal instanceof String s && !"anonymousUser".equals(s)) {
				MDC.put("user", s);
			}
		}
	}

	private static String headerOrDefault(HttpServletRequest req, String name, String def) {
		String v = req.getHeader(name);
		return (v == null || v.isEmpty()) ? def : v;
	}

	private static String extractClientIp(HttpServletRequest request) {
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


