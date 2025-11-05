package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "campaign_account", indexes = {
    @Index(name = "idx_ca_campaign", columnList = "campaign_id"),
    @Index(name = "idx_ca_account", columnList = "social_account_id")
})
@Getter
@Setter
@NoArgsConstructor
public class CampaignAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_account_id", nullable = false)
    private SocialMediaAccount socialAccount;


}


