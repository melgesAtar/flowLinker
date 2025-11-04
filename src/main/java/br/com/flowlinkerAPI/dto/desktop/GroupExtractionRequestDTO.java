package br.com.flowlinkerAPI.dto.desktop;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupExtractionRequestDTO {
    private String accountUsername;    
    private Instant extractedAt;       
    private List<String> keywords;     
    private List<SimpleGroupDTO> groups; 

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleGroupDTO {
        private String externalId; 
        private String name;
        private String url;
        private Long memberCount;
    }
}


