package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.dto.campaign.*;
import br.com.flowlinkerAPI.service.FacebookGroupShareCampaignService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private final FacebookGroupShareCampaignService fbService;

    public CampaignController(FacebookGroupShareCampaignService fbService) {
        this.fbService = fbService;
    }

    // Iniciar campanha de compartilhamento em grupos do Facebook
    @PostMapping("/facebook/group-share/start")
    public ResponseEntity<FacebookGroupShareStartResponse> startFacebookGroupShare(@Valid @RequestBody FacebookGroupShareStartRequest req) {
        return ResponseEntity.ok(fbService.start(req));
    }

    // Pausar campanha
    @PostMapping("/{campaignId}/pause")
    public ResponseEntity<Void> pause(@PathVariable Long campaignId, @RequestBody(required = false) CampaignProgressUpdateRequest progress) {
        fbService.pause(campaignId, progress);
        return ResponseEntity.noContent().build();
    }

    // Concluir campanha
    @PostMapping("/{campaignId}/complete")
    public ResponseEntity<Void> complete(@PathVariable Long campaignId, @RequestBody(required = false) CampaignProgressUpdateRequest progress) {
        fbService.complete(campaignId, progress);
        return ResponseEntity.noContent().build();
    }

    // Cancelar campanha
    @PostMapping("/{campaignId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long campaignId, @RequestBody(required = false) CampaignProgressUpdateRequest progress) {
        fbService.cancel(campaignId, progress);
        return ResponseEntity.noContent().build();
    }

    // Consultar progresso (para retomar de onde parou)
    @GetMapping("/{campaignId}/progress")
    public ResponseEntity<CampaignProgressResponse> progress(@PathVariable Long campaignId) {
        return ResponseEntity.ok(fbService.progress(campaignId));
    }

    // Listar campanhas RUNNING do device atual, com dados completos para retomar
    @GetMapping("/device/running")
    public ResponseEntity<java.util.List<CampaignResumeResponse>> runningForDevice() {
        return ResponseEntity.ok(fbService.listRunningForCurrentDevice());
    }
}


