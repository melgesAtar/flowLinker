package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stripe.model.checkout.Session;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
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

        return customerRepository.save(target);
    }


    private Customer.OfferType mapPriceIdToOfferType(String productId) {
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
}
