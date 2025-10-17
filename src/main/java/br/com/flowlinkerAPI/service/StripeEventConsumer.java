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
    public void processEvent(String eventJson) {
        logger.info("Receiving event in JSON from Stripe: {}", eventJson);

        Event event;

        try {
            event = ApiResource.GSON.fromJson(eventJson, Event.class);
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
                String productId = null;
                String priceId = null;

                try{
                    var params = com.stripe.param.checkout.SessionListLineItemsParams.builder().setLimit(1L).build();
                    var items = session.listLineItems(params);
                    if(items != null && !items.getData().isEmpty()) {
                        var item = items.getData().get(0);
                        if (item.getPrice() != null && item.getPrice().getProduct() != null) {
                            priceId = item.getPrice().getId();
                            productId = item.getPrice().getProduct();
                        }
                        // Agora productId estará preenchido se disponível.
                    }
                } catch (Exception e) {
                    logger.error("Error listing line items: {}", e.getMessage());
                }

                var customer = customerService.upsertFromStripeCheckout(session, productId, priceId);
                var user = userService.createOrGetUserByEmail(customer.getEmail());
                user.setCustomer(customer);
                userService.saveUser(user);

                logger.info("Customer and user created successfully: {}", customer.getEmail());
                break;
                

            }
            case "payment_intent.succeeded": {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                logger.info("PaymentIntent succeeded: {}", paymentIntent != null ? paymentIntent.getId() : "unknown");
                break;
            }
            default:
                logger.info("Unhandled event type: {}", event.getType());
        }
    }

    
}
