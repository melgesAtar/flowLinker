package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.GroupExtraction;
import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GroupExtractionRepository extends JpaRepository<GroupExtraction, Long> {
    List<GroupExtraction> findByCustomerOrderByExtractedAtDesc(Customer customer);
    List<GroupExtraction> findByDeviceOrderByExtractedAtDesc(Device device);

    Page<GroupExtraction> findByCustomerOrderByExtractedAtDesc(Customer customer, Pageable pageable);
}


