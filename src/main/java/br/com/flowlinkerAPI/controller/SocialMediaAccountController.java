package br.com.flowlinkerAPI.controller;

import org.springframework.web.bind.annotation.*;
import br.com.flowlinkerAPI.service.SocialMediaAccountService;
import org.springframework.http.ResponseEntity;
import java.util.List;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountResponse;
import br.com.flowlinkerAPI.dto.desktop.SocialCookieDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.flowlinkerAPI.config.security.CurrentUser;

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

    @GetMapping("/{id}/cookies")
    public ResponseEntity<List<SocialCookieDTO>> getCookies(@PathVariable Long id,
                                                            @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(service.getCookies(id, user.customerId()));
    }
}