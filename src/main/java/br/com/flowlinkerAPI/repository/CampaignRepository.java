package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.Campaign;
import br.com.flowlinkerAPI.model.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findAllByCustomerId(Long customerId);
    List<Campaign> findAllByCustomerIdAndStatus(Long customerId, CampaignStatus status);
    List<Campaign> findAllByStartedByDeviceIdAndStatus(Long deviceId, CampaignStatus status);
    List<Campaign> findAllByStartedByDeviceIdAndStatusIn(Long deviceId, List<CampaignStatus> statuses);

    @Query("select c from Campaign c where c.startedByDevice.id = :deviceId and c.type.code = :typeCode and c.status in :statuses")
    List<Campaign> findByDeviceAndTypeCodeAndStatuses(
        @Param("deviceId") Long deviceId,
        @Param("typeCode") String typeCode,
        @Param("statuses") List<CampaignStatus> statuses
    );
}


