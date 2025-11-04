package br.com.flowlinkerAPI.dto.desktop;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionSummaryDTO {
    private Long id;
    private Instant extractedAt;
    private Integer groupsCount;
    private String keywords; // texto simples
    private String socialAccountUsername;
    private String socialAccountPlatform;
    private String deviceId;
    private String deviceFingerprint;
}


