package br.com.flowlinkerAPI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.flowlinkerAPI.config.RabbitMQConfig;
import com.stripe.model.*;
import com.google.gson.JsonSyntaxException;
import com.stripe.net.ApiResource;
import org.springframework.stereotype.Service;
import br.com.flowlinkerAPI.repository.StripeProcessedEventRepository;
import br.com.flowlinkerAPI.model.StripeProcessedEvent;
import java.time.Instant;
import com.stripe.param.checkout.SessionListLineItemsParams;
@Service
public class StripeEventConsumer {

    private final Logger logger = LoggerFactory.getLogger(StripeEventConsumer.class);
    private final StripeProcessedEventRepository stripeProcessedEventRepository;
    private final CustomerService customerService;
    private final UserService userService;

    public StripeEventConsumer(StripeProcessedEventRepository stripeProcessedEventRepository, CustomerService customerService, UserService userService) {
        this.stripeProcessedEventRepository = stripeProcessedEventRepository;
        this.customerService = customerService;
        this.userService = userService;
    }
    
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processEvent(String payload) {
        logger.info("Payload received in consumer: {}", payload);

        Event event;

        try {
            event = ApiResource.GSON.fromJson(payload, Event.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Payload invalid from Stripe: {}", e.getMessage());
            return;
        }

        String id = event.getId();
        if (id == null) {
            logger.warn("Event without ID, ignoring.");
            return;
        }

        try{
            StripeProcessedEvent stripeProcessedEvent = new StripeProcessedEvent();
            stripeProcessedEvent.setEventId(id);
            stripeProcessedEvent.setEventType(event.getType());
            stripeProcessedEvent.setProcessedAt(Instant.now());
            stripeProcessedEventRepository.save(stripeProcessedEvent);
            logger.info("Event saved successfully: {}", id);

        } catch (DataIntegrityViolationException e) {
            logger.error("Event already processed: {}", id);
            return;
        }

        logger.info("Event converted to object: {}", event.getType());

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);


        if (stripeObject == null) {
            logger.warn("Stripe object not found in event");
            return;
        }
        
        
        switch (event.getType()) {
            case "checkout.session.completed": {

                com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObject;

                var customer = customerService.upsertFromStripeCheckout(session);
                var user = userService.createOrGetUserByEmail(customer.getEmail());
                user.setCustomer(customer);
                userService.saveUser(user);

                logger.info("Customer and user created successfully: {}", customer.getEmail());
                break;

            }
            case "invoice.paid": {
                logger.info("Invoice paid event received");
                Invoice invoice = (Invoice) stripeObject;

                if(!invoice.getBillingReason().equals("subscription_create")) {
                    Customer customer = customerService.getCustomerByCustomerId(customerId);
                    customer.setSubscriptionStatus(Customer.SubscriptionStatus.ACTIVE);
                    customerService.saveCustomer(customer);
                    logger.info("Customer subscription status updated to active: {}", customer.getEmail());
                } else {
                    logger.info("Invoice not related to a subscription, ignoring");
                    return;
                }


                break;
            }
            default:
                logger.info("Unhandled event type: {}", event.getType());
        }
    }

    
}
