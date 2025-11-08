package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_product_stripe", columnList = "stripe_product_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stripe_product_id", unique = true, nullable = false, length = 64)
    private String stripeProductId;

    @Column(name = "name", length = 128)
    private String name;
    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private Type type = Type.PLAN; // PLAN or ADDON

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "devices_per_unit")
    private Integer devicesPerUnit = 1;

    public enum Type { PLAN, ADDON }
}


