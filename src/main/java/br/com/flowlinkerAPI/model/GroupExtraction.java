package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_extraction", indexes = {
    @Index(name = "idx_ge_customer", columnList = "customer_id"),
    @Index(name = "idx_ge_extracted_at", columnList = "extracted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device; // dispositivo que realizou a extração

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer; // redundante, mas simples para consulta

    @Column(name = "extracted_at")
    private Instant extractedAt; // horário informado pelo cliente

    @Column(name = "keywords_text", length = 2048)
    private String keywordsText; // palavras/frases usadas (separadas por vírgula)

    @Column(name = "groups_count")
    private Integer groupsCount; // quantidade reportada/derivada

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_account_id")
    private SocialMediaAccount socialAccount; // conta que realizou a extração (opcional)

    @OneToMany(mappedBy = "extraction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupExtractionItem> items = new ArrayList<>();
}


