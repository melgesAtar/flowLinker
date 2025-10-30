package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.model.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByFingerprint(String fingerprint);
    List<Device> findByCustomerId(Long customerId);
    int countByCustomerId(Long customerId);

    int countByCustomerIdAndStatus(Long customerId, DeviceStatus status);
    List<Device> findByCustomerIdAndStatus(Long customerId, DeviceStatus status);
    Optional<Device> findByCustomerIdAndFingerprint(Long customerId, String fingerprint);
    Optional<Device> findByCustomerIdAndDeviceId(Long customerId, String deviceId);
    
}
