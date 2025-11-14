package br.com.flowlinkerAPI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricsProxyService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsProxyService.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REF =
            new ParameterizedTypeReference<>() {};

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
    private final ConcurrentHashMap<String, CacheEntry<Map<String, Object>>> sharesCache = new ConcurrentHashMap<>();

    // TTLs (ms)
    private static final long OVERVIEW_TTL_MS = 10_000L;
    private static final long RECENT_TTL_MS = 5_000L;
    private static final long SHARES_TTL_MS = 5_000L;

    public MetricsProxyService(
            @Value("${metrics.api.baseUrl:https://flowlinker-events.onrender.com}") String baseUrl
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        // RestTemplate simples (sem bean global) com timeouts razoáveis via system properties (padrões do JDK)
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> getSharesCount(Long customerId, Integer hours) {
        String key = customerId + ":" + String.valueOf(hours != null ? hours : 24);
        CacheEntry<Map<String, Object>> cached = sharesCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        int h = (hours != null ? hours : 24);
        Map<String, Object> body = getMapFromEvents("/metrics/shares/count", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
        long shares = toLong(body.get("shares"));
        Map<String, Object> safe = new HashMap<>();
        safe.put("customerId", customerId);
        safe.put("hours", h);
        safe.put("shares", shares);
        sharesCache.put(key, new CacheEntry<>(safe, SHARES_TTL_MS));
        return safe;
    }

    public Map<String, Object> getOverview(Long customerId, Integer hours) {
        String key = customerId + ":" + String.valueOf(hours != null ? hours : 24);
        CacheEntry<Map<String, Object>> cached = overviewCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        int h = (hours != null ? hours : 24);
        Map<String, Object> body = getMapFromEvents("/metrics/overview", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
        Map<String, Object> safe = new HashMap<>();
        if (body != null) {
            safe.putAll(body);
        }
        safe.put("customerId", customerId);
        safe.put("hours", h);
        overviewCache.put(key, new CacheEntry<>(safe, OVERVIEW_TTL_MS));
        return safe;
    }

    public List<Map<String, Object>> getRecent(Long customerId, Integer limit, String tz) {
        int lim = limit != null ? Math.max(1, Math.min(limit, 100)) : 20;
        String zone = (tz != null && !tz.isBlank()) ? tz : "UTC";
        String key = customerId + ":" + lim + ":" + zone;
        CacheEntry<List<Map<String, Object>>> cached = recentCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        List<Map<String, Object>> items = getListFromEvents("/metrics/recent", Map.of(
                "customerId", String.valueOf(customerId),
                "limit", String.valueOf(lim),
                "tz", zone
        ));
        recentCache.put(key, new CacheEntry<>(items, RECENT_TTL_MS));
        return items;
    }

    public Object getRecentRaw(Long customerId, Integer limit, String tz) {
        int lim = limit != null ? Math.max(1, Math.min(limit, 100)) : 20;
        String zone = (tz != null && !tz.isBlank()) ? tz : "UTC";
        return getObjectFromEvents("/metrics/recent", Map.of(
                "customerId", String.valueOf(customerId),
                "limit", String.valueOf(lim),
                "tz", zone
        ));
    }

    public Map<String, Object> getActionsSummary(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getMapFromEvents("/metrics/actions/summary", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    public Map<String, Object> getErrors(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getMapFromEvents("/metrics/errors", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    public Map<String, Object> getPeopleReached(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getMapFromEvents("/metrics/people-reached", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    public Object getDebugAccountCreated(Long customerId, Integer limit, String tz) {
        int lim = limit != null ? Math.max(1, Math.min(limit, 100)) : 20;
        String zone = (tz != null && !tz.isBlank()) ? tz : "UTC";
        return getObjectFromEvents("/metrics/debug/account-created", Map.of(
                "customerId", String.valueOf(customerId),
                "limit", String.valueOf(lim),
                "tz", zone
        ));
    }

    public Object getExtractionEvents(Long customerId, Integer limit, String tz) {
        int lim = limit != null ? Math.max(1, Math.min(limit, 100)) : 20;
        String zone = (tz != null && !tz.isBlank()) ? tz : "UTC";
        return getObjectFromEvents("/metrics/extractions/events", Map.of(
                "customerId", String.valueOf(customerId),
                "limit", String.valueOf(lim),
                "tz", zone
        ));
    }

    public Map<String, Object> getDistributionSocial(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getMapFromEvents("/metrics/distribution/social", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    public Object getDaily(Long customerId, Integer days) {
        int d = days != null ? days : 7;
        return getObjectFromEvents("/metrics/daily", Map.of(
                "customerId", String.valueOf(customerId),
                "days", String.valueOf(d)
        ));
    }

    public Object getHeatmap(Long customerId, Integer days) {
        int d = days != null ? days : 7;
        return getObjectFromEvents("/metrics/heatmap", Map.of(
                "customerId", String.valueOf(customerId),
                "days", String.valueOf(d)
        ));
    }

    public Object getRankingPersonas(Long customerId, Integer hours, Integer limit) {
        int h = hours != null ? hours : 24;
        int lim = limit != null ? Math.max(1, Math.min(limit, 100)) : 10;
        return getObjectFromEvents("/metrics/ranking/personas", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h),
                "limit", String.valueOf(lim)
        ));
    }

    public Map<String, Object> getCampaignsCount(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getMapFromEvents("/metrics/campaigns/count", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    @NonNull
    private URI buildUri(String path, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(baseUrl);
        if (!path.startsWith("/")) {
            sb.append("/");
        }
        sb.append(path.startsWith("/") ? path.substring(1) : path);
        if (params != null && !params.isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() == null) continue;
                if (!first) sb.append("&");
                first = false;
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        return Objects.requireNonNull(URI.create(sb.toString()));
    }

    private Map<String, Object> getMapFromEvents(String path, Map<String, String> params) {
        try {
            URI uri = Objects.requireNonNull(buildUri(path, params));
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    uri,
                    Objects.requireNonNull(HttpMethod.GET),
                    null,
                    Objects.requireNonNull(MAP_TYPE_REF)
            );
            Map<String, Object> map = resp.getBody();
            if (map != null) {
                return new HashMap<>(map);
            }
        } catch (Exception e) {
            logger.warn("Falha ao consultar {}: {}", path, e.getMessage());
        }
        return Collections.emptyMap();
    }

    private List<Map<String, Object>> getListFromEvents(String path, Map<String, String> params) {
        try {
            URI uri = Objects.requireNonNull(buildUri(path, params));
            ResponseEntity<Object> resp = restTemplate.getForEntity(uri, Object.class);
            Object body = resp.getBody();
            if (body instanceof List<?> list) {
                return list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> {
                            Map<?, ?> src = (Map<?, ?>) item;
                            Map<String, Object> copy = new HashMap<>();
                            src.forEach((k, v) -> copy.put(String.valueOf(k), v));
                            return copy;
                        })
                        .toList();
            }
            if (body instanceof Map<?, ?> map && map.get("items") instanceof List<?> list) {
                return list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> {
                            Map<?, ?> src = (Map<?, ?>) item;
                            Map<String, Object> copy = new HashMap<>();
                            src.forEach((k, v) -> copy.put(String.valueOf(k), v));
                            return copy;
                        })
                        .toList();
            }
        } catch (Exception e) {
            logger.warn("Falha ao consultar lista {}: {}", path, e.getMessage());
        }
        return Collections.emptyList();
    }

    private Object getObjectFromEvents(String path, Map<String, String> params) {
        try {
            URI uri = Objects.requireNonNull(buildUri(path, params));
            ResponseEntity<Object> resp = restTemplate.getForEntity(uri, Object.class);
            return resp.getBody();
        } catch (Exception e) {
            logger.warn("Falha ao consultar {}: {}", path, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }
}


