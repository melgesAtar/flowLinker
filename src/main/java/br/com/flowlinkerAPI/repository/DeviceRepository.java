package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByFingerprint(String fingerprint);
    List<Device> findByCustomerId(Long customerId);
    int countByCustomerId(Long customerId);
    void deleteByCustomerId(Long customerId);
}
