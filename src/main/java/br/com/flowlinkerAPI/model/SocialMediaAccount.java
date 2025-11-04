package br.com.flowlinkerAPI.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.LocalDateTime;
import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.model.Device;
import lombok.Getter;
import lombok.Setter;
@Entity
@Getter
@Setter
public class SocialMediaAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private SocialMediaPlatform platform;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String username;
    private String password;
    
    private String name;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String cookiesJson;

    private LocalDateTime coookiesExpiry;

    @Enumerated(EnumType.STRING)
    private SocialMediaAccountStatus status;
    

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device; // device que cadastrou a conta (se veio do desktop)

    public enum SocialMediaAccountStatus {
        ACTIVE,
        INACTIVE,
        BLOCKED,
        DELETED,
        SUSPENDED
    }

    public enum SocialMediaPlatform {
        FACEBOOK,
        INSTAGRAM,
        TWITTER,
        YOUTUBE,
        TIKTOK,
        LINKEDIN,
    }
}
