package br.com.flowlinkerAPI.config.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import br.com.flowlinkerAPI.config.security.CurrentUser;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


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
                String role = claims.get("role") != null ? String.valueOf(claims.get("role")) : "USER";
                List<GrantedAuthority> authorities = new ArrayList<>();
                if (role != null && !role.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                }
                CurrentUser principal = new CurrentUser(username, customerId, deviceFp, role);
                return new UsernamePasswordAuthenticationToken(principal, null, authorities);
            }
        }
        return null;
    }

  
    @Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.startsWith("/auth/login")
        || path.startsWith("/auth/password/")
        || path.startsWith("/stripe/")
        || "OPTIONS".equalsIgnoreCase(request.getMethod());
}
}
