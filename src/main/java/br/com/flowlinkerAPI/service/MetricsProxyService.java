package br.com.flowlinkerAPI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricsProxyService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsProxyService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    private static class CacheEntry<T> {
        final T value;
        final long expiresAtEpochMs;
        CacheEntry(T value, long ttlMs) {
            this.value = value;
            this.expiresAtEpochMs = System.currentTimeMillis() + ttlMs;
        }
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtEpochMs;
        }
    }

    // caches simples com TTL curto
    private final ConcurrentHashMap<String, CacheEntry<Map<String, Object>>> overviewCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry<List<Map<String, Object>>>> recentCache = new ConcurrentHashMap<>();

    // TTLs (ms)
    private static final long OVERVIEW_TTL_MS = 10_000L;
    private static final long RECENT_TTL_MS = 5_000L;

    public MetricsProxyService(
            @Value("${metrics.api.baseUrl:http://localhost:9090}") String baseUrl
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        // RestTemplate simples (sem bean global) com timeouts razoáveis via system properties (padrões do JDK)
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> getOverview(Long customerId, Integer hours) {
        String key = customerId + ":" + String.valueOf(hours != null ? hours : 24);
        CacheEntry<Map<String, Object>> cached = overviewCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        int h = (hours != null ? hours : 24);
        try {
            // 1) People reached
            URI peopleUri = URI.create(baseUrl + "/metrics/people-reached?customerId=" + customerId + "&hours=" + h);
            ResponseEntity<Map> peopleResp = restTemplate.getForEntity(peopleUri, Map.class);
            long peopleReached = 0L;
            if (peopleResp.getBody() != null) {
                peopleReached = toLong(peopleResp.getBody().get("peopleReached"));
            }
    
            // 2) Actions summary
            URI actionsUri = URI.create(baseUrl + "/metrics/actions/summary?customerId=" + customerId + "&hours=" + h);
            ResponseEntity<Map> actionsResp = restTemplate.getForEntity(actionsUri, Map.class);
            long total = 0L;
            Long shares = 0L, extractions = 0L, instagramLikes = 0L, instagramComments = 0L;
            if (actionsResp.getBody() != null) {
                Map body = actionsResp.getBody();
                shares = toLong(body.get("shares"));
                extractions = toLong(body.get("extractions"));
                instagramLikes = toLong(body.get("instagramLikes"));
                instagramComments = toLong(body.get("instagramComments"));
                total = toLong(body.get("total"));
            }
    
            // Saída compatível com seu card (mantém campos que o front espera)
            Map<String, Object> safe = new HashMap<>();
            safe.put("customerId", customerId);
            safe.put("hours", h);
            safe.put("totalActions", total);
            safe.put("peopleReached", peopleReached);
            // opcional: breakdown (útil para evoluir UI sem quebrar)
            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("shares", shares);
            breakdown.put("extractions", extractions);
            breakdown.put("instagramLikes", instagramLikes);
            breakdown.put("instagramComments", instagramComments);
            breakdown.put("total", total);
            safe.put("breakdown", breakdown);
    
            overviewCache.put(key, new CacheEntry<>(safe, OVERVIEW_TTL_MS));
            return safe;
        } catch (Exception e) {
            logger.warn("Falha ao consultar endpoints de metrics (people-reached/actions): {}", e.getMessage());
            Map<String, Object> fallback = Map.of(
                    "customerId", customerId,
                    "hours", h,
                    "totalActions", 0L,
                    "peopleReached", 0L
            );
            overviewCache.put(key, new CacheEntry<>(fallback, OVERVIEW_TTL_MS));
            return fallback;
        }
    }

    public List<Map<String, Object>> getRecent(Long customerId, Integer limit) {
        String key = customerId + ":" + String.valueOf(limit != null ? limit : 20);
        CacheEntry<List<Map<String, Object>>> cached = recentCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        try {
            int lim = limit != null ? Math.max(1, Math.min(limit, 100)) : 20;
            // Força timezone UTC na API de métricas
            URI uri = URI.create(baseUrl + "/metrics/recent?customerId=" + customerId + "&limit=" + lim + "&tz=UTC");
            ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
            Map<String, Object> body = resp.getBody();
            List<Map<String, Object>> items;
            if (body != null && body.get("items") instanceof List list) {
                // garantimos apenas os campos necessários
                items = (List<Map<String, Object>>) list;
                items = items.stream().map(this::normalizeActivity).toList();
            } else {
                items = Collections.emptyList();
            }
            recentCache.put(key, new CacheEntry<>(items, RECENT_TTL_MS));
            return items;
        } catch (Exception e) {
            logger.warn("Falha ao consultar recent no 9090: {}", e.getMessage());
            List<Map<String, Object>> fallback = Collections.emptyList();
            recentCache.put(key, new CacheEntry<>(fallback, RECENT_TTL_MS));
            return fallback;
        }
    }

    private Map<String, Object> normalizeActivity(Map<String, Object> raw) {
        Map<String, Object> m = new HashMap<>();
        m.put("eventAt", raw.getOrDefault("eventAt", Instant.now().toString()));
        m.put("actor", raw.getOrDefault("actor", null));
        m.put("text", raw.getOrDefault("text", null));
        return m;
    }

    private Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }
}


