package br.com.flowlinkerAPI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import br.com.flowlinkerAPI.config.RabbitMQConfig;
import com.stripe.model.*;
import com.google.gson.JsonSyntaxException;
import com.stripe.net.ApiResource;
import org.springframework.stereotype.Service;
import br.com.flowlinkerAPI.repository.StripeProcessedEventRepository;
import br.com.flowlinkerAPI.model.StripeProcessedEvent;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;
import br.com.flowlinkerAPI.service.email.EmailService;
import br.com.flowlinkerAPI.exceptions.WelcomeEmailNotSendException;

@Service
public class StripeEventConsumer {

    private final Logger logger = LoggerFactory.getLogger(StripeEventConsumer.class);
    private final StripeProcessedEventRepository stripeProcessedEventRepository;
    private final CustomerService customerService;
    private final UserService userService;
    private final StripeProcessedEventService stripeProcessedEventService;
    private final SubscriptionSyncService subscriptionSyncService;
    private final EmailService emailService;
    private final String stripeSecretKey;
    
    public StripeEventConsumer(StripeProcessedEventRepository stripeProcessedEventRepository, CustomerService customerService, UserService userService, StripeProcessedEventService stripeProcessedEventService, EmailService emailService, SubscriptionSyncService subscriptionSyncService, @org.springframework.beans.factory.annotation.Value("${stripe.api.key:}") String stripeSecretKey) {
        this.stripeProcessedEventRepository = stripeProcessedEventRepository;
        this.customerService = customerService;
        this.userService = userService;
        this.stripeProcessedEventService = stripeProcessedEventService;
        this.emailService = emailService;
        this.subscriptionSyncService = subscriptionSyncService;
        this.stripeSecretKey = stripeSecretKey;
    }
   
    
    @Transactional
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processEvent(String payload) {
        logger.info("Stripe event received");

        Event event;

        try {
            event = ApiResource.GSON.fromJson(payload, Event.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Invalid payload from Stripe: {}", e.getMessage());
            return;
        }

        String id = event.getId();
        if (id == null) {
            logger.warn("Event without ID, ignoring.");
            return;
        }
        
    
            StripeProcessedEvent stripeProcessedEvent = stripeProcessedEventService.getStripeProcessedEventByEventId(id);
            if (stripeProcessedEvent != null) {
                logger.info("Event already processed: {}, skipping", id);
                return;
            }
            logger.info("New event: {}, processing", id);
      

        logger.info("Stripe event type: {}", event.getType());

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
                var result = userService.createOrGetUserWithPasswordReturn(customer.getEmail());
                var user = result.getUser();
                user.setCustomer(customer);
                userService.saveUser(user);

                // Tenta sincronizar a assinatura imediatamente se presente no session
                try {
                    String subId = session.getSubscription();
                    if (subId != null && !subId.isEmpty() && stripeSecretKey != null && !stripeSecretKey.isEmpty()) {
                        com.stripe.Stripe.apiKey = stripeSecretKey;
                        com.stripe.model.Subscription sub = com.stripe.model.Subscription.retrieve(subId);
                        if (sub != null) {
                            subscriptionSyncService.upsertFromStripe(sub);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to sync subscription on checkout.session.completed: {}", e.getMessage());
                }

                if(result.isNewUser()) {
                   try {
                    emailService.sendWelcomeEmail(customer.getEmail(), user.getUsername(), result.getPlainPassword());
                   } catch (WelcomeEmailNotSendException e) {
                    logger.error("Error sending welcome email: {}", e.getMessage());
                   }
                }
                
                logger.info("Customer and user created successfully: {}", customer.getEmail());
                break;

            }
            case "invoice.paid": {
                logger.info("Invoice paid event received");
                Invoice invoice = (Invoice) stripeObject;
                customerService.updateCustomerFromInvoice(invoice);
                break;
            }
            case "invoice.payment_failed": {
                logger.info("Invoice payment failed event received");
                Invoice invoice = (Invoice) stripeObject;
                customerService.updateCustomerFromInvoice(invoice);
                logger.info("Customer subscription payment failed: {}", invoice.getCustomer());
                break;
            }
            case "invoice.payment_succeeded": {
                logger.info("Invoice payment succeeded event received");
                Invoice invoice = (Invoice) stripeObject;
                customerService.updateCustomerFromInvoice(invoice);
                logger.info("Customer subscription payment succeeded: {}", invoice.getCustomer());
                break;
            }
            case "invoice.payment_action_required": {
                logger.info("Invoice payment action required event received");
                Invoice invoice = (Invoice) stripeObject;
                customerService.updateCustomerFromInvoice(invoice);
                logger.info("Customer subscription payment action required: {}", invoice.getCustomer());
                break;
            }
            
            case "customer.subscription.created":{
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                subscriptionSyncService.upsertFromStripe(subscription);
                customerService.updateCustomerFromSubscription(subscription);
                logger.info("Customer subscription created: {}", subscription.getCustomer());
                break;
            }
            case "customer.subscription.updated":{
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                subscriptionSyncService.upsertFromStripe(subscription);
                customerService.updateCustomerFromSubscription(subscription);
                logger.info("Customer subscription updated: {}", subscription.getCustomer());
                break;
            }
            case "customer.subscription.deleted":{
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                subscriptionSyncService.upsertFromStripe(subscription);
                customerService.updateCustomerFromSubscription(subscription);
                logger.info("Customer subscription deleted: {}", subscription.getCustomer());
                break;
            }
            case "customer.subscription.paused":{
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                subscriptionSyncService.upsertFromStripe(subscription);
                customerService.updateCustomerFromSubscription(subscription);
                logger.info("Customer subscription paused: {}", subscription.getCustomer());
                break;
            }
            case "customer.subscription.resumed":{
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                subscriptionSyncService.upsertFromStripe(subscription);
                customerService.updateCustomerFromSubscription(subscription);
                logger.info("Customer subscription resumed: {}", subscription.getCustomer());
                break;
            }
            case "customer.subscription.unpaused":{
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                subscriptionSyncService.upsertFromStripe(subscription);
                customerService.updateCustomerFromSubscription(subscription);
                logger.info("Customer subscription unpaused: {}", subscription.getCustomer());
                break;
            }
            case "customer.subscription.trial_will_end":{
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                subscriptionSyncService.upsertFromStripe(subscription);
                customerService.updateCustomerFromSubscription(subscription);
                logger.info("Customer subscription trial will end: {}", subscription.getCustomer());
                break;
            }
            
            default:
                logger.info("Unhandled event type: {}", event.getType());
        }

            stripeProcessedEvent = new StripeProcessedEvent();
            stripeProcessedEvent.setEventId(id);
            stripeProcessedEvent.setEventType(event.getType());
            stripeProcessedEvent.setProcessedAt(Instant.now());
            stripeProcessedEventRepository.save(stripeProcessedEvent);
            logger.info("Event saved successfully: {}", id);
    }

    
}
