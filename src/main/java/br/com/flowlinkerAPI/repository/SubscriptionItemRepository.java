package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.SubscriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionItemRepository extends JpaRepository<SubscriptionItem, Long> {
    Optional<SubscriptionItem> findByStripeItemId(String stripeItemId);
    List<SubscriptionItem> findBySubscriptionId(Long subscriptionId);
}


