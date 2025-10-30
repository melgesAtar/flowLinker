package br.com.flowlinkerAPI.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Setter;
import lombok.Getter;
import jakarta.persistence.PrePersist;

@Entity
@Table(
  name = "device",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_device_customer_fingerprint", columnNames = {"customer_id", "fingerprint"}),
    @UniqueConstraint(name = "uk_device_customer_deviceid", columnNames = {"customer_id", "deviceId"})
  }
)
@Setter
@Getter
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    private String deviceId;
    private String deviceSecret;

    private String fingerprint;
    private String hwHashBaseline;
    private String lastHwHash;
    private String osName;
    private String osVersion;
    private String arch;
    private String hostname;
    private String appVersion;
    private String lastIp;
    private String name;
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private DeviceStatus status;

    private Instant createdAt;

    private Instant lastSeenAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
