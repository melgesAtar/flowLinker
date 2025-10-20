package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stripe.model.checkout.Session;
import com.stripe.model.Invoice;
import java.time.Instant;
import com.stripe.model.Subscription;
import java.util.Map;
import java.util.HashMap;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    private final DeviceService deviceService; 
    private static final Map<Customer.OfferType, Integer> MAX_DEVICES = new HashMap<>();
    static {
        MAX_DEVICES.put(Customer.OfferType.BASIC, 3);
        MAX_DEVICES.put(Customer.OfferType.STANDARD, 5);
        MAX_DEVICES.put(Customer.OfferType.PRO, 10);
    }

    
    public CustomerService(CustomerRepository customerRepository, DeviceService deviceService) {
        this.customerRepository = customerRepository;
        this.deviceService = deviceService;
    }

    @Transactional
    private void adjustDevicesOnPlanChange(Customer customer, Customer.OfferType oldOfferType, Customer.OfferType newOfferType) {
       
        if(oldOfferType == newOfferType) {
            return;
        }

        int oldLimit = MAX_DEVICES.getOrDefault(oldOfferType, 0);
        int newLimit = MAX_DEVICES.getOrDefault(customer.getOfferType(), 0);
       
        if(newLimit < oldLimit) {
            deviceService.deleteByCustomerId(customer.getId());
            logger.info("Downgrade detected: all devices deleted for customer {}", customer.getEmail());
        }else{
            logger.info("Upgrade detected: devices kept, new limit {} for customer {}", newLimit, customer.getEmail());
        }
    }

    public Customer upsertFromStripeCheckout(Session session) {
        
        String email = session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null;
        String name = session.getCustomerDetails() != null ? session.getCustomerDetails().getName() : null;
        String phoneNumber = session.getCustomerDetails() != null ? session.getCustomerDetails().getPhone() : null;
        String address1 = session.getCustomerDetails() != null ? session.getCustomerDetails().getAddress().getLine1() : null;
        String address2 = session.getCustomerDetails() != null ? session.getCustomerDetails().getAddress().getLine2() : null;
        String city = session.getCustomerDetails() != null ? session.getCustomerDetails().getAddress().getCity() : null;
        String state = session.getCustomerDetails() != null ? session.getCustomerDetails().getAddress().getState() : null;
        String postalCode = session.getCustomerDetails() != null ? session.getCustomerDetails().getAddress().getPostalCode() : null;
        String country = session.getCustomerDetails() != null ? session.getCustomerDetails().getAddress().getCountry() : null;

        String stripeCustomerId = session.getCustomer() != null ? session.getCustomer() : null;
        String stripeSubscriptionId = session.getSubscription() != null ? session.getSubscription() : null;

        
        Customer existingCustomer = null;
        if(stripeCustomerId != null) {
            existingCustomer = customerRepository.findAll().stream()
            .filter(c -> stripeCustomerId.equals(c.getStripeCustomerId()))
            .findFirst()
            .orElse(null);
        }

        if(existingCustomer == null && email != null) {
            existingCustomer = customerRepository.findByEmail(email).orElse(null);
        }

        Customer target = existingCustomer != null ? existingCustomer : new Customer();

        target.setEmail(email);
        target.setName(name);
        target.setPhoneNumber(phoneNumber);
        target.setAddressLine1(address1);
        target.setAddressLine2(address2);
        target.setCity(city);
        target.setState(state);
        target.setPostalCode(postalCode);
        target.setStripeCustomerId(stripeCustomerId);
        target.setStripeSubscriptionId(stripeSubscriptionId);
        target.setCountry(country);
        logger.info("Customer created: {}", target.getEmail());
        return customerRepository.save(target);
    }


    public void updateCustomerFromInvoice(Invoice invoice) {

        if (invoice == null) {
            logger.warn("Invoice is null, ignoring update");
            return;
        }

        String stripeCustomerId = invoice.getCustomer();
        if (stripeCustomerId == null) {
            logger.warn("Customer ID not found in invoice");
            return;
        }

        Customer customer = customerRepository.findByStripeCustomerId(stripeCustomerId).orElse(null);
        if (customer == null) {
            logger.warn("Customer not found for ID: {}", stripeCustomerId);
            return;
        }

        String subscriptionId = null;
        if (!invoice.getLines().getData().isEmpty()) {
            var line = invoice.getLines().getData().get(0);
            var parent = line.getParent();
            if (parent != null) {
                if ("subscription_details".equals(parent.getType())) {
                    subscriptionId = parent.getSubscriptionItemDetails().getSubscription();
                } else if ("invoice_item_details".equals(parent.getType()) && parent.getInvoiceItemDetails() != null) {
                    subscriptionId = parent.getInvoiceItemDetails().getSubscription();
                }
            }
        }
        if (subscriptionId != null) {
            customer.setStripeSubscriptionId(subscriptionId);
        } else {
            logger.warn("No subscription ID found in invoice lines for customer {}", customer.getEmail());
        }

        if (!invoice.getLines().getData().isEmpty()) {
            var line = invoice.getLines().getData().get(0);
            customer.setStripePriceId(line.getPricing().getPriceDetails().getPrice() != null ? line.getPricing().getPriceDetails().getPrice() : customer.getStripePriceId());
            customer.setStripeProductId(line.getPricing().getPriceDetails().getProduct() != null ? line.getPricing().getPriceDetails().getProduct() : customer.getStripeProductId());
            customer.setOfferType(mapPriceIdToOfferType(line.getPricing().getPriceDetails().getProduct() != null ? line.getPricing().getPriceDetails().getPrice() : customer.getStripeProductId()));
            
            adjustDevicesOnPlanChange(customer, customer.getOfferType(), mapPriceIdToOfferType(line.getPricing().getPriceDetails().getProduct() != null ? line.getPricing().getPriceDetails().getPrice() : customer.getStripeProductId()));
        } else {
            logger.warn("Invoice without lines, keeping existing price/product");
        }
        customer.setCollectionMethodStripe(invoice.getCollectionMethod() != null ? invoice.getCollectionMethod() : customer.getCollectionMethodStripe());
        customer.setSubscriptionStartDate(invoice.getPeriodStart() != null ? Instant.ofEpochSecond(invoice.getPeriodStart()) : customer.getSubscriptionStartDate());
        customer.setSubscriptionEndDate(invoice.getPeriodEnd() != null ? Instant.ofEpochSecond(invoice.getPeriodEnd()) : customer.getSubscriptionEndDate());

        String status = invoice.getStatus();
        switch (status != null ? status : "unknown") {
            case "paid":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.ACTIVE);
                logger.info("Invoice paid: activating subscription for {}", customer.getEmail());
                break;
            case "open":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.PENDING);
                logger.info("Invoice open: subscription pending for {}", customer.getEmail());
                break;
            case "draft":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.PENDING);  // Ou INCOMPLETE, dependendo da lógica
                logger.info("Invoice in draft: subscription pending for {}", customer.getEmail());
                break;
            case "void":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.CANCELED);
                logger.info("Invoice void: canceling subscription for {}", customer.getEmail());
                break;
            case "uncollectible":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.UNPAID);
                logger.warn("Invoice uncollectible: subscription unpaid for {}", customer.getEmail());
                break;
            default:  
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.PENDING);  // Default seguro
                logger.warn("Unknown or unhandled invoice status ({}): setting as pending for {}", status, customer.getEmail());
                break;
        }

        customerRepository.save(customer);
        logger.info("Customer updated from invoice: {}", customer.getEmail());
    }




    public void updateCustomerFromSubscription(Subscription subscription) {

        if (subscription == null) {
            logger.warn("Subscription is null, ignoring update");
            return;
        }

        String stripeCustomerId = subscription.getCustomer();
        if (stripeCustomerId == null) {
            logger.warn("Customer ID not found in subscription");
            return;
        }

        Customer customer = customerRepository.findByStripeCustomerId(stripeCustomerId).orElse(null);
    if (customer == null) {
        logger.warn("Customer not found for ID: {}, creating new from subscription", stripeCustomerId);
        customer = new Customer();
        customer.setStripeCustomerId(stripeCustomerId);
        
    }

        
        customer.setStripeSubscriptionId(subscription.getId() != null ? subscription.getId() : customer.getStripeSubscriptionId());  

        if (!subscription.getItems().getData().isEmpty()) {
            var item = subscription.getItems().getData().get(0);
            customer.setSubscriptionStartDate(item.getCurrentPeriodStart() != null ? Instant.ofEpochSecond(item.getCurrentPeriodStart()) : customer.getSubscriptionStartDate());
            customer.setSubscriptionEndDate(item.getCurrentPeriodEnd() != null ? Instant.ofEpochSecond(item.getCurrentPeriodEnd()) : customer.getSubscriptionEndDate());

            if (item.getPrice() != null) {
                customer.setStripePriceId(item.getPrice().getId() != null ? item.getPrice().getId() : customer.getStripePriceId());
                customer.setStripeProductId(item.getPrice().getProduct() != null ? item.getPrice().getProduct() : customer.getStripeProductId());
                customer.setOfferType(mapPriceIdToOfferType(customer.getStripeProductId()));  // Mapeia offerType baseado no product
            } else {
                logger.warn("Item without price, keeping existing price/product");
            }

            if (item.getPlan() != null) {
                customer.setPlanIntervalStripe(mapPlanInterval(item.getPlan().getInterval() != null ? item.getPlan().getInterval() : null));
            } else {
                logger.warn("Item without plan, keeping existing interval");
            }
        } else {
            logger.warn("Subscription without items, keeping existing dates/price/product/interval");
        }

        customer.setCollectionMethodStripe(subscription.getCollectionMethod() != null ? subscription.getCollectionMethod() : customer.getCollectionMethodStripe());

        String status = subscription.getStatus();
        switch (status != null ? status : "unknown") {
            case "active":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.ACTIVE);
                logger.info("Subscription active: updating to ACTIVE for {}", customer.getEmail());
                break;
            case "trialing":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.TRIALING);
                logger.info("Subscription in trial: updating to TRIALING for {}", customer.getEmail());
                break;
            case "past_due":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.PAST_DUE);
                logger.warn("Subscription past due: updating to PAST_DUE for {}", customer.getEmail());
                break;
            case "canceled":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.CANCELED);
                logger.info("Subscription canceled: updating to CANCELED for {}", customer.getEmail());
                break;
            case "unpaid":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.UNPAID);
                logger.warn("Subscription unpaid: updating to UNPAID for {}", customer.getEmail());
                break;
            case "incomplete":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.INCOMPLETE);
                logger.warn("Subscription incomplete: updating to INCOMPLETE for {}", customer.getEmail());
                break;
            case "incomplete_expired":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.INCOMPLETE_EXPIRED);
                logger.warn("Subscription incomplete expired: updating to INCOMPLETE_EXPIRED for {}", customer.getEmail());
                break;
            case "paused":
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.PAUSED);
                logger.info("Subscription paused: updating to PAUSED for {}", customer.getEmail());
                break;
            default:  // Cobre "pending" (se existir), desconhecidos, ou qualquer outro
                customer.setSubscriptionStatus(Customer.SubscriptionStatus.PENDING);  // Default seguro
                logger.warn("Unknown or unhandled subscription status ({}): setting as PENDING for {}", status, customer.getEmail());
                break;
        }
  
        saveCustomer(customer);
        logger.info("Customer updated from subscription: {}", customer.getEmail());
    }

    


    public Customer.OfferType mapPriceIdToOfferType(String productId) {
        if(productId == null) {
           return Customer.OfferType.BASIC;
        }

        switch(productId) {
            case "prod_TF3HMUtwQ6KVta":
                return Customer.OfferType.BASIC;
            case "prod_TF3NychpFF1g1j":
                return Customer.OfferType.STANDARD;
            case "prod_TF3UmhzoX6KOY0":
                return Customer.OfferType.PRO;
            default:
                return Customer.OfferType.BASIC;
        }
    }
    public Customer.PlanInterval mapPlanInterval(String planInterval) {
       if(planInterval == null) {
        return Customer.PlanInterval.MONTHLY;
       }
       switch(planInterval) {
        case "month":
            return Customer.PlanInterval.MONTHLY;
        case "year":
            return Customer.PlanInterval.YEARLY;
       }
       return Customer.PlanInterval.MONTHLY;
    }

    
    public Customer getCustomerByStripeCustomerId(String stripeCustomerId) {
        return customerRepository.findByStripeCustomerId(stripeCustomerId).orElse(null);
    }
    public void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }
}
