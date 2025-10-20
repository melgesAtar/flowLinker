package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.List;

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
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Instant subscriptionStartDate;
    private Instant subscriptionEndDate;

    @Enumerated(EnumType.STRING)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus subscriptionStatus;

    public enum SubscriptionStatus { TRIALING, ACTIVE, INCOMPLETE, INCOMPLETE_EXPIRED, PAST_DUE, CANCELED, UNPAID, PENDING, PAUSED, OPEN}


    public enum OfferType {BASIC, STANDARD, PRO}

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private User user;

    @jakarta.persistence.Column(unique = true)
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripePriceId;
    private String stripeProductId;

    private String collectionMethodStripe;
    
    @Enumerated(EnumType.STRING)
    private PlanInterval planIntervalStripe;
    public enum PlanInterval {MONTHLY, YEARLY}
    

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Device> devices;

    @Override
    public String toString() {
        return "Customer{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
