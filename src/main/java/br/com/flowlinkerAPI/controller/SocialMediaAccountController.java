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
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountBasicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

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

    // Novo endpoint: aceita nenhuma, uma ou várias plataformas (?platform=FACEBOOK&platform=INSTAGRAM)
    // Se não vier plataforma, retorna todas as contas (exceto DELETED) do cliente.
    @Operation(
        summary = "Buscar contas por plataformas (opcional)",
        description = "Retorna as contas de redes sociais do cliente autenticado. "
                    + "Se nenhum parâmetro 'platform' for enviado, retorna todas as contas (exceto DELETED). "
                    + "Aceita múltiplos 'platform' na query string, por exemplo: ?platform=FACEBOOK&platform=INSTAGRAM."
    )
    @GetMapping("/search")
    public ResponseEntity<List<SocialMediaAccountResponse>> search(
            @Parameter(description = "Lista de plataformas (FACEBOOK, INSTAGRAM, etc.). Se ausente, retorna todas as contas.")
            @RequestParam(name = "platform", required = false) java.util.List<String> platforms,
            @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(service.listAccountsByPlatforms(platforms, user.customerId()));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<SocialMediaAccountResponse>> listByStatus(@RequestParam String platform,
                                                                         @RequestParam String status,
                                                                         @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(service.listByStatus(platform, status, user.customerId()));
    }

    @GetMapping("/available")
    public ResponseEntity<List<SocialMediaAccountResponse>> availableForCampaign(@RequestParam String platform,
                                                                                 @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(service.listActiveNotInRunningOrPausedCampaign(platform, user.customerId()));
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

        @GetMapping("/mine")
        public ResponseEntity<List<SocialMediaAccountBasicResponse>> listMine(@AuthenticationPrincipal CurrentUser user) {
            return ResponseEntity.ok(service.listMineBasic(user.customerId()));
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