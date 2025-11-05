package br.com.flowlinkerAPI.dto.campaign;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class FacebookGroupShareStartRequest {
    @NotNull
    public Long extractionId; // lista imutável usada

    @NotNull
    public List<Long> accountIds; // contas selecionadas para rotação

    public String message; // opcional
    public String linkUrl; // opcional

    public Integer rotateAccountEveryNShares;
    public Integer typingDelayMs;
    public Integer postIntervalDelayMs;
    public Integer clickButtonsDelayMs;
}


