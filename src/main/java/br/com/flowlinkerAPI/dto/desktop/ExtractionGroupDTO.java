package br.com.flowlinkerAPI.dto.desktop;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionGroupDTO {
    private Long groupId;
    private String externalId;
    private String name;
    private String url;
    private Long memberCount;
    private Instant lastSeenAt;
}


