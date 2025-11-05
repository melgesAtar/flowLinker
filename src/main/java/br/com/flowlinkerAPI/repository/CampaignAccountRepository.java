package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.CampaignAccount;
import br.com.flowlinkerAPI.model.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CampaignAccountRepository extends JpaRepository<CampaignAccount, Long> {
    List<CampaignAccount> findAllByCampaignId(Long campaignId);

    @Query("select ca from CampaignAccount ca where ca.socialAccount.id in :accountIds and ca.campaign.status in :statuses")
    List<CampaignAccount> findByAccountIdsAndCampaignStatuses(@Param("accountIds") List<Long> accountIds, @Param("statuses") List<CampaignStatus> statuses);
}


