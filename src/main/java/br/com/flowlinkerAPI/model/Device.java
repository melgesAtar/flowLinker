package br.com.flowlinkerAPI.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Setter;
import lombok.Getter;

@Entity
@Setter
@Getter
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    private String fingerprint;
    private String name;
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
