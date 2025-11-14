package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.GroupCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupCatalogRepository extends JpaRepository<GroupCatalog, Long> {
    Optional<GroupCatalog> findByExternalId(String externalId);
    Optional<GroupCatalog> findByUrl(String url);

    List<GroupCatalog> findByExternalIdIn(Collection<String> externalIds);
    List<GroupCatalog> findByUrlIn(Collection<String> urls);
}


