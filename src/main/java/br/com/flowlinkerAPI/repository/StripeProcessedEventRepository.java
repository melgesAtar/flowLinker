package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.StripeProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StripeProcessedEventRepository extends JpaRepository<StripeProcessedEvent, String> {
    Optional<StripeProcessedEvent> findByEventId(String eventId);
}
