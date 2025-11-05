package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(
    name = "campaign_type",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_campaigntype_code", columnNames = {"code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class CampaignType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code; // ex.: FB_GROUP_SHARE, MASS_DIRECT, FB_LIKE, FB_COMMENT

    @Column(nullable = false, length = 255)
    private String name; // ex.: "Compartilhamento em grupos facebook"

    private Instant createdAt = Instant.now();
}


