package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fb_group_share_campaign")
@Getter
@Setter
@NoArgsConstructor
public class FacebookGroupShareCampaign {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private Campaign campaign;

    // Lista imutável utilizada pela campanha: referencia a extração fonte
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_id")
    private GroupExtraction extractionUsed;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String message;

    @Column(length = 1024)
    private String linkUrl;

    private Integer rotateAccountEveryNShares; // troca a cada X compartilhamentos

    private Integer typingDelayMs; // delay ao digitar em ms

    private Integer postIntervalDelayMs; // delay entre postagens

    private Integer clickButtonsDelayMs; // delay ao clicar os botões

    // Progresso: índice do último grupo processado (0-based). Próximo = lastProcessedIndex + 1
    private Integer lastProcessedIndex = 0;
}


