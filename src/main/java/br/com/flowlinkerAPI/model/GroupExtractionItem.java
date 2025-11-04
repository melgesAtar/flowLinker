package br.com.flowlinkerAPI.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "group_extraction_item",
    uniqueConstraints = @UniqueConstraint(name = "uk_extraction_group", columnNames = {"extraction_id", "group_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupExtractionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extraction_id", nullable = false)
    private GroupExtraction extraction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupCatalog group;
}


