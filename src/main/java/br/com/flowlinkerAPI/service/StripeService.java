package br.com.flowlinkerAPI.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    private final Logger logger = LoggerFactory.getLogger(StripeService.class);
    private final String endpointSecret;

    public StripeService(@Value("${webhook.secret}") String endpointSecret) {
        this.endpointSecret = endpointSecret;
    }

    public ResponseEntity<Void> handleWebhook(String payload, String sigHeader) {
        if (sigHeader == null) {
            return ResponseEntity.status(400).build();
        }

        Event event;
        logger.info("Payload do Stripe: {}", payload);
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            logger.warn("Assinatura inválida do Stripe: {}", e.getMessage());
            return ResponseEntity.status(400).build();
        } catch (RuntimeException e) {
            logger.warn("Payload inválido do Stripe: {}", e.getMessage());
            return ResponseEntity.status(400).build();
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

        switch (event.getType()) {
            case "payment_intent.succeeded": {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                logger.info("PaymentIntent succeeded: {}", paymentIntent != null ? paymentIntent.getId() : "unknown");
                break;
            }
            case "payment_method.attached": {
                PaymentMethod paymentMethod = (PaymentMethod) stripeObject;
                logger.info("PaymentMethod attached: {}", paymentMethod != null ? paymentMethod.getId() : "unknown");
                break;
            }
            default:
                logger.info("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.noContent().build();
    }
}