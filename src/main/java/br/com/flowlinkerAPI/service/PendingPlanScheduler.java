package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class PendingPlanScheduler {

    private final Logger logger = LoggerFactory.getLogger(PendingPlanScheduler.class);
    private final CustomerRepository customerRepository;
    private final DevicePolicyService devicePolicyService;
    private final DeviceService deviceService;

    public PendingPlanScheduler(CustomerRepository customerRepository,
                                DevicePolicyService devicePolicyService,
                                DeviceService deviceService) {
        this.customerRepository = customerRepository;
        this.devicePolicyService = devicePolicyService;
        this.deviceService = deviceService;
    }

    // Executa diariamente às 03:15
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void applyPendingPlans() {
        Instant now = Instant.now();
        List<Customer> customers = customerRepository.findByPendingOfferTypeNotNullAndPendingOfferEffectiveAtBefore(now);
        for (Customer c : customers) {
            var oldOffer = c.getOfferType();
            var newOffer = c.getPendingOfferType();
            c.setOfferType(newOffer);
            c.setPendingOfferType(null);
            c.setPendingOfferEffectiveAt(null);
            customerRepository.save(c);
            logger.info("Applied pending plan {} -> {} for customer {}", oldOffer, newOffer, c.getEmail());
            // Se novo limite for menor que anterior, desativar devices excedentes
            int newLimit = devicePolicyService.getAllowedDevices(c.getId(), newOffer);
            // Estrategicamente, desativamos todos e o cliente reautentica devices dentro do novo limite
            deviceService.deactivateByCustomerId(c.getId());
            logger.info("Devices reset for customer {} after plan change. New limit: {}", c.getEmail(), newLimit);
        }
    }
}


