package br.com.flowlinkerAPI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Service
public class MetricsProxyService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsProxyService.class);
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public MetricsProxyService(
            @Value("${metrics.api.baseUrl:https://flowlinker-events.onrender.com}") String baseUrl
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        // RestTemplate simples (sem bean global) com timeouts razoáveis via system properties (padrões do JDK)
        this.restTemplate = new RestTemplate();
    }

    public Object getSharesCount(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getObjectFromEvents("/metrics/shares/count", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    public Object getOverview(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getObjectFromEvents("/metrics/overview", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    public Object getRecent(Long customerId, Integer limit, String tz) {
        int lim = limit != null ? Math.max(1, Math.min(limit, 100)) : 20;
        String zone = (tz != null && !tz.isBlank()) ? tz : "UTC";
        return getObjectFromEvents("/metrics/recent", Map.of(
                "customerId", String.valueOf(customerId),
                "limit", String.valueOf(lim),
                "tz", zone
        ));
    }

    public Object getRecentRaw(Long customerId, Integer limit, String tz) {
        return getRecent(customerId, limit, tz);
    }

    public Object getActionsSummary(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getObjectFromEvents("/metrics/actions/summary", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    public Object getErrors(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getObjectFromEvents("/metrics/errors", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    public Object getPeopleReached(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getObjectFromEvents("/metrics/people-reached", Map.of(
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

    public Object getDistributionSocial(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getObjectFromEvents("/metrics/distribution/social", Map.of(
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

    public Object getCampaignsCount(Long customerId, Integer hours) {
        int h = hours != null ? hours : 24;
        return getObjectFromEvents("/metrics/campaigns/count", Map.of(
                "customerId", String.valueOf(customerId),
                "hours", String.valueOf(h)
        ));
    }

    @NonNull
    private URI buildUri(String path, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(baseUrl);
        if (!baseUrl.endsWith("/")) {
            sb.append("/");
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        sb.append(normalized);
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

    private Object getObjectFromEvents(String path, Map<String, String> params) {
        try {
            URI uri = Objects.requireNonNull(buildUri(path, params));
            ResponseEntity<Object> resp = restTemplate.getForEntity(uri, Object.class);
            Object body = resp.getBody();
            logger.info("MetricsProxy → {} params={} response={}", path, params, body);
            return body;
        } catch (Exception e) {
            logger.warn("Falha ao consultar {}: {}", path, e.getMessage());
            return Collections.emptyList();
        }
    }
}


