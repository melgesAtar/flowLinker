package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.model.Product;
import br.com.flowlinkerAPI.model.Subscription;
import br.com.flowlinkerAPI.model.SubscriptionItem;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import br.com.flowlinkerAPI.repository.ProductRepository;
import br.com.flowlinkerAPI.repository.SubscriptionItemRepository;
import br.com.flowlinkerAPI.repository.SubscriptionRepository;
import com.stripe.model.Price;
import com.stripe.model.SubscriptionItemCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class SubscriptionSyncService {

    private final Logger logger = LoggerFactory.getLogger(SubscriptionSyncService.class);
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionItemRepository subscriptionItemRepository;
    private final DevicePolicyService devicePolicyService;

    public SubscriptionSyncService(CustomerRepository customerRepository,
                                   ProductRepository productRepository,
                                   SubscriptionRepository subscriptionRepository,
                                   SubscriptionItemRepository subscriptionItemRepository,
                                   DevicePolicyService devicePolicyService) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionItemRepository = subscriptionItemRepository;
        this.devicePolicyService = devicePolicyService;
    }

    @Transactional
    public void upsertFromStripe(com.stripe.model.Subscription stripeSub) {
        String stripeCustomerId = stripeSub.getCustomer();
        if (stripeCustomerId == null) {
            logger.warn("Stripe subscription without customer, skipping");
            return;
        }
        Customer customer = customerRepository.findByStripeCustomerId(stripeCustomerId).orElse(null);
        if (customer == null) {
            logger.warn("Customer not found for stripeCustomerId={}, skipping", stripeCustomerId);
            return;
        }

        Subscription sub = subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId()).orElse(null);
        if (sub == null) {
            sub = new Subscription();
            sub.setStripeSubscriptionId(stripeSub.getId());
            sub.setCustomer(customer);
        }
        sub.setStatus(stripeSub.getStatus());
        sub.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSub.getCancelAtPeriodEnd()));
        sub.setDefaultPaymentMethodId(stripeSub.getDefaultPaymentMethod());
        sub.setCurrency(stripeSub.getCurrency());
        sub.setUpdatedAt(Instant.now());
        subscriptionRepository.save(sub);

        SubscriptionItemCollection items = stripeSub.getItems();
        if (items != null && items.getData() != null) {
            Long minStart = null;
            Long maxEnd = null;
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            String interval = null;
            Integer intervalCount = null;
            for (com.stripe.model.SubscriptionItem si : items.getData()) {
                String stripeItemId = si.getId();
                String productId = null;
                BigDecimal unit = null;
                Integer quantity = si.getQuantity() != null ? si.getQuantity().intValue() : 1;
                if (si.getPrice() != null) {
                    Price price = si.getPrice();
                    productId = price.getProduct();
                    if (price.getUnitAmount() != null) {
                        unit = BigDecimal.valueOf(price.getUnitAmount()).movePointLeft(2);
                    }
                    if (price.getCurrency() != null) {
                        sub.setCurrency(price.getCurrency());
                    }
                    if (price.getRecurring() != null) {
                        interval = price.getRecurring().getInterval();
                        var ic = price.getRecurring().getIntervalCount();
                        intervalCount = ic != null ? ic.intValue() : null;
                    }
                } else if (si.getPlan() != null) {
                    productId = si.getPlan().getProduct();
                    if (si.getPlan().getAmount() != null) {
                        unit = BigDecimal.valueOf(si.getPlan().getAmount()).movePointLeft(2);
                    }
                    if (si.getPlan().getCurrency() != null) {
                        sub.setCurrency(si.getPlan().getCurrency());
                    }
                    if (si.getPlan().getInterval() != null) {
                        interval = si.getPlan().getInterval();
                        var ic2 = si.getPlan().getIntervalCount();
                        intervalCount = ic2 != null ? ic2.intValue() : null;
                    }
                }
                if (si.getCurrentPeriodStart() != null) {
                    minStart = (minStart == null) ? si.getCurrentPeriodStart() : Math.min(minStart, si.getCurrentPeriodStart());
                }
                if (si.getCurrentPeriodEnd() != null) {
                    maxEnd = (maxEnd == null) ? si.getCurrentPeriodEnd() : Math.max(maxEnd, si.getCurrentPeriodEnd());
                }
                if (productId == null) {
                    logger.warn("SubscriptionItem without product, skipping {}", stripeItemId);
                    continue;
                }
                final String pid = productId;
                Product product = productRepository.findByStripeProductId(pid).orElseGet(() -> {
                    Product p = new Product();
                    p.setStripeProductId(pid);
                    // Heurística: se id do produto igual ao do plano do customer ⇒ PLAN; senão ADDON
                    Product.Type type = Product.Type.ADDON;
                    if (customer.getStripeProductId() != null && customer.getStripeProductId().equals(pid)) {
                        type = Product.Type.PLAN;
                    }
                    p.setType(type);
                    // Devices per unit: plan uses fallback from offerType; addon = 1
                    if (type == Product.Type.PLAN) {
                        int fallback = devicePolicyService.getMaxDevices(customer.getOfferType());
                        p.setDevicesPerUnit(Math.max(fallback, 1));
                    } else {
                        p.setDevicesPerUnit(1);
                    }
                    return productRepository.save(p);
                });

                SubscriptionItem entity = subscriptionItemRepository.findByStripeItemId(stripeItemId).orElseGet(SubscriptionItem::new);
                entity.setStripeItemId(stripeItemId);
                entity.setSubscription(sub);
                entity.setProduct(product);
                entity.setQuantity(quantity != null ? quantity : 1);
                entity.setUnitPrice(unit);
                entity.setCurrency(sub.getCurrency());
                entity.setBillingInterval(interval);
                entity.setBillingIntervalCount(intervalCount);
                if (si.getCurrentPeriodStart() != null) {
                    entity.setCurrentPeriodStart(Instant.ofEpochSecond(si.getCurrentPeriodStart()));
                }
                if (si.getCurrentPeriodEnd() != null) {
                    entity.setCurrentPeriodEnd(Instant.ofEpochSecond(si.getCurrentPeriodEnd()));
                }
                subscriptionItemRepository.save(entity);

                if (unit != null && quantity != null) {
                    total = total.add(unit.multiply(BigDecimal.valueOf(quantity)));
                }
            }
            if (minStart != null) sub.setCurrentPeriodStart(Instant.ofEpochSecond(minStart));
            if (maxEnd != null) sub.setCurrentPeriodEnd(Instant.ofEpochSecond(maxEnd));
            sub.setAmountTotal(total);
            if (interval != null) sub.setBillingInterval(interval);
            if (intervalCount != null) sub.setBillingIntervalCount(intervalCount);
            sub.setUpdatedAt(Instant.now());
            subscriptionRepository.save(sub);
        }
        logger.info("Subscription sync completed for customer {} ({})", customer.getEmail(), stripeSub.getId());
    }
}


