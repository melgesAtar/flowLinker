package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.Product;
import br.com.flowlinkerAPI.model.Subscription;
import br.com.flowlinkerAPI.model.SubscriptionItem;
import br.com.flowlinkerAPI.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

@Service
public class EntitlementService {

    private final SubscriptionRepository subscriptionRepository;

    public EntitlementService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public int getAllowedDevices(Long customerId) {
        if (customerId == null) return 0;
        var subscriptions = subscriptionRepository.findByCustomerId(customerId);
        int total = 0;
        for (Subscription sub : subscriptions) {
            if (sub.getItems() == null) continue;
            for (SubscriptionItem item : sub.getItems()) {
                Product p = item.getProduct();
                if (p == null) continue;
                Integer dpu = p.getDevicesPerUnit() != null ? p.getDevicesPerUnit() : 0;
                Integer qty = item.getQuantity() != null ? item.getQuantity() : 0;
                total += dpu * qty;
            }
        }
        return total;
    }
}


