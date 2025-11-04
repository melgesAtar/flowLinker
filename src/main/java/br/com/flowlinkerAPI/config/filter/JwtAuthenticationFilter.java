package br.com.flowlinkerAPI.config.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import java.io.IOException;
import java.util.ArrayList;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import jakarta.servlet.http.Cookie;
import br.com.flowlinkerAPI.config.security.CurrentUser;


public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    private final RedisTemplate<String, String> redisTemplate;
   
    private String jwtSecret;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, RedisTemplate<String, String> redisTemplate, String jwtSecret) {
        super(authenticationManager);
        this.redisTemplate = redisTemplate;
        this.jwtSecret = jwtSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String header = request.getHeader("Authorization");
        String token = null;

        if(header != null && header.startsWith("Bearer ")) {
            token = header.replace("Bearer ", "");
        }else{
            Cookie[] cookies = request.getCookies();
            if(cookies != null) {
                for(Cookie cookie : cookies) {
                    if("jwtToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(request, token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request, String token) { 
        
        String type = request.getHeader("X-Auth-Type"); 
        String fingerprint = request.getHeader("X-Fingerprint"); 

       
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

       
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String redisKey;
        if ("device".equals(claims.get("type"))) {
            if (fingerprint == null || !fingerprint.equals(claims.get("fingerprint"))) {
                return null;  
            }
            Object customerIdClaim = claims.get("customerId");
            if (customerIdClaim == null) {
                return null;
            }
            redisKey = "device:token:" + customerIdClaim + ":" + fingerprint;
        } else {
            redisKey = (type != null ? type : "web") + ":token:" + claims.getSubject();
        }

        String storedToken = redisTemplate.opsForValue().get(redisKey);

        if (storedToken != null && storedToken.equals(token)) {
            if (claims.getSubject() != null) {
                String username = claims.getSubject();
                Long customerId = null;
                Object cid = claims.get("customerId");
                if (cid instanceof Number) {
                    customerId = ((Number) cid).longValue();
                } else if (cid instanceof String) {
                    try { customerId = Long.parseLong((String) cid); } catch (Exception ignored) {}
                }
                String tokenType = String.valueOf(claims.get("type"));
                String deviceFp = "device".equals(tokenType) ? fingerprint : null;
                CurrentUser principal = new CurrentUser(username, customerId, deviceFp);
                return new UsernamePasswordAuthenticationToken(principal, null, new ArrayList<>());
            }
        }
        return null;
    }

  
    @Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.startsWith("/auth/login")
        || path.startsWith("/stripe/")
        || "OPTIONS".equalsIgnoreCase(request.getMethod());
}
}
