package br.com.flowlinkerAPI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import br.com.flowlinkerAPI.config.RabbitMQConfig;
import com.stripe.model.*;
import com.google.gson.JsonSyntaxException;
import com.stripe.net.ApiResource;

public class StripeEventConsumer {
    private final Logger logger = LoggerFactory.getLogger(StripeEventConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processEvent(String eventJson) {
        logger.info("Recebendo evento em JSON Stripe: {}", eventJson);

        Event event;

        try {
            event = ApiResource.GSON.fromJson(eventJson, Event.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Payload inválido do Stripe: {}", e.getMessage());
            return;
        }

        logger.info("Evento convertido para objeto: {}", event.getType());

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);


        if (stripeObject == null) {
            logger.warn("Objeto Stripe não encontrado no evento");
            return;
        }
        
        
        switch (event.getType()) {
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
