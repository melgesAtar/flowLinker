package br.com.flowlinkerAPI.service;   

import br.com.flowlinkerAPI.repository.StripeProcessedEventRepository;
import br.com.flowlinkerAPI.model.StripeProcessedEvent;
import org.springframework.stereotype.Service;

@Service        
public class StripeProcessedEventService {
    private final StripeProcessedEventRepository stripeProcessedEventRepository;

    public StripeProcessedEventService(StripeProcessedEventRepository stripeProcessedEventRepository) {
        this.stripeProcessedEventRepository = stripeProcessedEventRepository;
    }

    public void saveStripeProcessedEvent(StripeProcessedEvent stripeProcessedEvent) {
        stripeProcessedEventRepository.save(stripeProcessedEvent);
    }

    public StripeProcessedEvent getStripeProcessedEventByEventId(String id) {
        return stripeProcessedEventRepository.findByEventId(id).orElse(null);
    }
}
