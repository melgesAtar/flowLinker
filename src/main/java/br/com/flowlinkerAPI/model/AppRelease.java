package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "app_release",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_release_platform_arch_version", columnNames = {"platform", "arch", "version"})
    },
    indexes = {
        @Index(name = "idx_app_release_active_created", columnList = "is_active, created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class AppRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Arch arch;

    @Column(nullable = false, length = 32)
    private String version; // SemVer

    @Column(name = "minimum_version", length = 32)
    private String minimumVersion; // SemVer mínima suportada

    @Column(nullable = false, length = 512)
    private String downloadUrl;

    @Column(length = 128)
    private String sha256;

    private Long fileSize;

    @Column(name = "force_update")
    private Boolean forceUpdate = Boolean.FALSE;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String releaseNotes;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public enum Platform { WIN, MAC, LINUX }
    public enum Arch { X64, ARM64 }
}


