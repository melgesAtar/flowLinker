package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.FacebookGroupShareCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacebookGroupShareCampaignRepository extends JpaRepository<FacebookGroupShareCampaign, Long> {
}


