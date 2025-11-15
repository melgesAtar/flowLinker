package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.config.security.CurrentUser;
import br.com.flowlinkerAPI.service.MetricsProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
@Tag(name = "Metrics", description = "Proxy dos endpoints do serviço externo de métricas/eventos")
public class MetricsProxyController {

    private final MetricsProxyService service;

    public MetricsProxyController(MetricsProxyService service) {
        this.service = service;
    }

    @Operation(summary = "Overview", description = "Proxy para o endpoint /metrics/overview da API de eventos.")
    @GetMapping("/overview")
    public ResponseEntity<Object> overview(@AuthenticationPrincipal CurrentUser user,
                                           @Parameter(description = "Janela em horas") @RequestParam(defaultValue = "24") Integer hours) {
        return ResponseEntity.ok(service.getOverview(user.customerId(), hours));
    }

    @Operation(summary = "Total de compartilhamentos", description = "Proxy para /metrics/shares/count.")
    @GetMapping({"/shares/count", "/shares"})
    public ResponseEntity<Object> shares(@AuthenticationPrincipal CurrentUser user,
                                         @Parameter(description = "Janela em horas") @RequestParam(defaultValue = "24") Integer hours) {
        return ResponseEntity.ok(service.getSharesCount(user.customerId(), hours));
    }

    @Operation(summary = "Atividades recentes (formatado)", description = "Proxy para /metrics/recent com formatação compatível com o painel.")
    @GetMapping("/recent")
    public ResponseEntity<Object> recent(@AuthenticationPrincipal CurrentUser user,
                                         @Parameter(description = "Quantidade máxima de eventos") @RequestParam(defaultValue = "20") Integer limit,
                                         @Parameter(description = "Timezone (ex.: America/Sao_Paulo)") @RequestParam(required = false) String tz) {
        return ResponseEntity.ok(service.getRecent(user.customerId(), limit, tz));
    }

    @Operation(summary = "Atividades recentes (raw)", description = "Repassa exatamente o retorno do /metrics/recent da API de eventos.")
    @GetMapping("/recent/raw")
    public ResponseEntity<Object> recentRaw(@AuthenticationPrincipal CurrentUser user,
                                            @Parameter(description = "Quantidade máxima de eventos") @RequestParam(defaultValue = "20") Integer limit,
                                            @Parameter(description = "Timezone desejada") @RequestParam(required = false) String tz) {
        return ResponseEntity.ok(service.getRecentRaw(user.customerId(), limit, tz));
    }

    @Operation(summary = "Resumo de ações", description = "Proxy para /metrics/actions/summary.")
    @GetMapping("/actions/summary")
    public ResponseEntity<Object> actionsSummary(@AuthenticationPrincipal CurrentUser user,
                                                 @Parameter(description = "Janela em horas") @RequestParam(defaultValue = "24") Integer hours) {
        return ResponseEntity.ok(service.getActionsSummary(user.customerId(), hours));
    }

    @Operation(summary = "Resumo de erros", description = "Proxy para /metrics/errors.")
    @GetMapping("/errors")
    public ResponseEntity<Object> errors(@AuthenticationPrincipal CurrentUser user,
                                         @Parameter(description = "Janela em horas") @RequestParam(defaultValue = "24") Integer hours) {
        return ResponseEntity.ok(service.getErrors(user.customerId(), hours));
    }

    @Operation(summary = "Pessoas alcançadas", description = "Proxy para /metrics/people-reached.")
    @GetMapping("/people-reached")
    public ResponseEntity<Object> peopleReached(@AuthenticationPrincipal CurrentUser user,
                                                @Parameter(description = "Janela em horas") @RequestParam(defaultValue = "24") Integer hours) {
        return ResponseEntity.ok(service.getPeopleReached(user.customerId(), hours));
    }

    @Operation(summary = "Eventos de contas criadas (debug)", description = "Proxy para /metrics/debug/account-created.")
    @GetMapping("/debug/account-created")
    public ResponseEntity<Object> debugAccountCreated(@AuthenticationPrincipal CurrentUser user,
                                                      @Parameter(description = "Quantidade") @RequestParam(defaultValue = "20") Integer limit,
                                                      @Parameter(description = "Timezone") @RequestParam(required = false) String tz) {
        return ResponseEntity.ok(service.getDebugAccountCreated(user.customerId(), limit, tz));
    }

    @Operation(summary = "Eventos de extração", description = "Proxy para /metrics/extractions/events.")
    @GetMapping("/extractions/events")
    public ResponseEntity<Object> extractionEvents(@AuthenticationPrincipal CurrentUser user,
                                                   @Parameter(description = "Quantidade") @RequestParam(defaultValue = "20") Integer limit,
                                                   @Parameter(description = "Timezone") @RequestParam(required = false) String tz) {
        return ResponseEntity.ok(service.getExtractionEvents(user.customerId(), limit, tz));
    }

    @Operation(summary = "Distribuição por rede social", description = "Proxy para /metrics/distribution/social.")
    @GetMapping("/distribution/social")
    public ResponseEntity<Object> distributionSocial(@AuthenticationPrincipal CurrentUser user,
                                                     @Parameter(description = "Janela em horas") @RequestParam(defaultValue = "24") Integer hours) {
        return ResponseEntity.ok(service.getDistributionSocial(user.customerId(), hours));
    }

    @Operation(summary = "Séries diárias", description = "Proxy para /metrics/daily.")
    @GetMapping("/daily")
    public ResponseEntity<Object> daily(@AuthenticationPrincipal CurrentUser user,
                                        @Parameter(description = "Quantidade de dias") @RequestParam(defaultValue = "7") Integer days) {
        return ResponseEntity.ok(service.getDaily(user.customerId(), days));
    }

    @Operation(summary = "Heatmap horário", description = "Proxy para /metrics/heatmap.")
    @GetMapping("/heatmap")
    public ResponseEntity<Object> heatmap(@AuthenticationPrincipal CurrentUser user,
                                          @Parameter(description = "Quantidade de dias") @RequestParam(defaultValue = "7") Integer days) {
        return ResponseEntity.ok(service.getHeatmap(user.customerId(), days));
    }

    @Operation(summary = "Ranking de personas", description = "Proxy para /metrics/ranking/personas.")
    @GetMapping("/ranking/personas")
    public ResponseEntity<Object> rankingPersonas(@AuthenticationPrincipal CurrentUser user,
                                                  @Parameter(description = "Janela em horas") @RequestParam(defaultValue = "24") Integer hours,
                                                  @Parameter(description = "Quantidade") @RequestParam(defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(service.getRankingPersonas(user.customerId(), hours, limit));
    }

    @Operation(summary = "Campanhas iniciadas", description = "Proxy para /metrics/campaigns/count.")
    @GetMapping("/campaigns/count")
    public ResponseEntity<Object> campaignsCount(@AuthenticationPrincipal CurrentUser user,
                                                 @Parameter(description = "Janela em horas") @RequestParam(defaultValue = "24") Integer hours) {
        return ResponseEntity.ok(service.getCampaignsCount(user.customerId(), hours));
    }

    @Operation(summary = "Último dispositivo que usou a conta", description = "Proxy para /accounts/last-device na API de eventos.")
    @GetMapping("/accounts/last-device")
    public ResponseEntity<Object> lastDeviceForAccount(
            @AuthenticationPrincipal CurrentUser user,
            @Parameter(description = "ID do cliente (se não informado, usa o customerId do token)") @RequestParam(required = false) Long customerId,
            @Parameter(description = "E-mail da conta de rede social") @RequestParam String account,
            @Parameter(description = "Timezone (ex: America/Sao_Paulo)") @RequestParam(required = false) String tz
    ) {
        Long cid = (customerId != null) ? customerId : user.customerId();
        return ResponseEntity.ok(service.getLastDeviceForAccount(cid, account, tz));
    }
}


