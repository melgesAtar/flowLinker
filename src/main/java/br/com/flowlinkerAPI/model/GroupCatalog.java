package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.*;
import java.time.Instant;

@Entity
@Table(
    name = "group_catalog",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_external_id", columnNames = {"external_id"}),
        @UniqueConstraint(name = "uk_group_url", columnNames = {"url"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@BatchSize(size = 50)
public class GroupCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId; // ex.: facebook_group_id

    @Column(nullable = false, length = 512)
    private String name;

	@Column(nullable = false, length = 700)
    private String url;

    @Column(name = "member_count")
    private Long memberCount; // quantidade de pessoas no grupo (quando informada)

    @Column(name = "last_seen_at")
    private Instant lastSeenAt; // última vez que apareceu em uma extração
}


