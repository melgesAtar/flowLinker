package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "subscription", indexes = {
        @Index(name = "idx_subscription_stripe", columnList = "stripe_subscription_id", unique = true),
        @Index(name = "idx_subscription_customer", columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "stripe_subscription_id", unique = true, nullable = false, length = 64)
    private String stripeSubscriptionId;

    @Column(length = 32)
    private String status; // ACTIVE, CANCELED, etc (texto vindo do Stripe)

    @Column(name = "cancel_at_period_end")
    private Boolean cancelAtPeriodEnd = Boolean.FALSE;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;
    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "amount_total", precision = 12, scale = 2)
    private java.math.BigDecimal amountTotal;

    @Column(name = "default_payment_method_id", length = 64)
    private String defaultPaymentMethodId;

    @Column(name = "billing_interval", length = 16)
    private String billingInterval; // month, year, etc.

    @Column(name = "billing_interval_count")
    private Integer billingIntervalCount;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "trial_start")
    private Instant trialStart;

    @Column(name = "trial_end")
    private Instant trialEnd;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubscriptionItem> items;
}


