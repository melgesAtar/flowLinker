package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.CampaignType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CampaignTypeRepository extends JpaRepository<CampaignType, Long> {
    Optional<CampaignType> findByCode(String code);
}


