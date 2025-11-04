package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.GroupExtractionItem;
import br.com.flowlinkerAPI.model.GroupExtraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GroupExtractionItemRepository extends JpaRepository<GroupExtractionItem, Long> {
    List<GroupExtractionItem> findByExtraction(GroupExtraction extraction);
    Page<GroupExtractionItem> findByExtraction(GroupExtraction extraction, Pageable pageable);
}


