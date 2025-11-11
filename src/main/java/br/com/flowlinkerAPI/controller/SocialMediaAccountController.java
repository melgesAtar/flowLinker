package br.com.flowlinkerAPI.controller;

import org.springframework.web.bind.annotation.*;
import br.com.flowlinkerAPI.service.SocialMediaAccountService;
import org.springframework.http.ResponseEntity;
import java.util.List;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountResponse;
import br.com.flowlinkerAPI.dto.desktop.SocialCookieDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.flowlinkerAPI.config.security.CurrentUser;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountStatusPatch;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountCreateRequest;
import org.springframework.http.HttpStatus;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountUpdateRequest;

@RestController
@RequestMapping("/social-media-accounts")
public class SocialMediaAccountController {

    private final SocialMediaAccountService service;

    public SocialMediaAccountController(SocialMediaAccountService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SocialMediaAccountResponse>> list(@RequestParam String platform,
                                                                 @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(service.listAccountsForPlatform(platform, user.customerId()));
    }

    @GetMapping("/active/count")
    public ResponseEntity<java.util.Map<String, Object>> countActive(@RequestParam(required = false) String platform,
                                                                     @AuthenticationPrincipal CurrentUser user) {
        long count = service.countActive(user.customerId(), platform);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("customerId", user.customerId());
        resp.put("activeCount", count);
        if (platform != null) {
            resp.put("platform", platform);
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}/cookies")
    public ResponseEntity<List<SocialCookieDTO>> getCookies(@PathVariable Long id,
                                                            @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(service.getCookies(id, user.customerId()));
    }


    @PutMapping("/{id}/cookies")
    public ResponseEntity<Void> replaceCookies(@PathVariable Long id,
                                                @RequestBody List<SocialCookieDTO> cookies,
                                                @AuthenticationPrincipal CurrentUser user) {
        service.replaceCookies(id, user.customerId(), cookies);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<SocialMediaAccountResponse> patchStatus(@PathVariable Long id,
                                                                @RequestBody SocialMediaAccountStatusPatch body,
                                                                @AuthenticationPrincipal CurrentUser user) {
        var updated = service.updateStatus(id, user.customerId(), body);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<SocialMediaAccountResponse> block(@PathVariable Long id,
                                                            @AuthenticationPrincipal CurrentUser user) {
        var updated = service.blockAccount(id, user.customerId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping
    public ResponseEntity<SocialMediaAccountResponse> create(@RequestBody SocialMediaAccountCreateRequest body,
                                                            @AuthenticationPrincipal CurrentUser user) {
        var created = service.createAccount(body, user.customerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SocialMediaAccountResponse> update(@PathVariable Long id,
                                                            @RequestBody SocialMediaAccountUpdateRequest body,
                                                            @AuthenticationPrincipal CurrentUser user) {
        var updated = service.updateAccount(id, user.customerId(), body);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id,
                                        @AuthenticationPrincipal CurrentUser user) {
        service.softDelete(id, user.customerId());
        return ResponseEntity.noContent().build();
    }
}