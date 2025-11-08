package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "subscription_item", indexes = {
        @Index(name = "idx_subitem_stripe", columnList = "stripe_item_id", unique = true),
        @Index(name = "idx_subitem_subscription", columnList = "subscription_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "stripe_item_id", unique = true, nullable = false, length = 64)
    private String stripeItemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "billing_interval", length = 16)
    private String billingInterval; // month, year

    @Column(name = "billing_interval_count")
    private Integer billingIntervalCount;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;
    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;
}


