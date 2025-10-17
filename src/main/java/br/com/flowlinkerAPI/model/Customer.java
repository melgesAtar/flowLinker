package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Column(unique = true)
    private String email;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;
    private String documentNumber;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private LocalDateTime subscriptionEndDate;

    @Enumerated(EnumType.STRING)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus subscriptionStatus;

    public enum SubscriptionStatus {ACTIVE, PENDING, CANCELLED, PAST_DUE, TRIAL}

    public enum DocumentType {CPF, CNPJ}
    public enum OfferType {BASIC, STANDARD, PRO}

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private User user;

    @jakarta.persistence.Column(unique = true)
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripePriceId;
    private String stripeProductId;

    
    @Override
    public String toString() {
        return "Customer{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
