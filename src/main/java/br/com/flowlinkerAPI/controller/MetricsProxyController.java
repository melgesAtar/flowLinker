package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.config.security.CurrentUser;
import br.com.flowlinkerAPI.service.MetricsProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsProxyController {

    private final MetricsProxyService service;

    public MetricsProxyController(MetricsProxyService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview(@AuthenticationPrincipal CurrentUser user,
                                                        @RequestParam(defaultValue = "24") Integer hours) {
        Long customerId = user.customerId();
        Map<String, Object> body = service.getOverview(customerId, hours);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/shares")
    public ResponseEntity<Map<String, Object>> shares(@AuthenticationPrincipal CurrentUser user,
                                                      @RequestParam(defaultValue = "24") Integer hours) {
        Long customerId = user.customerId();
        Map<String, Object> body = service.getSharesCount(customerId, hours);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> recent(@AuthenticationPrincipal CurrentUser user,
                                                      @RequestParam(defaultValue = "20") Integer limit) {
        Long customerId = user.customerId();
        List<Map<String, Object>> items = service.getRecent(customerId, limit);
        Map<String, Object> out = new HashMap<>();
        out.put("customerId", customerId);
        out.put("items", items);
        return ResponseEntity.ok(out);
    }
}


