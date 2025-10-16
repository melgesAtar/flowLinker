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
import com.stripe.net.ApiResource;
import com.google.gson.JsonSyntaxException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import br.com.flowlinkerAPI.config.RabbitMQConfig;

@Service
public class StripeService {

    private final Logger logger = LoggerFactory.getLogger(StripeService.class);
    private final String endpointSecret;
    private final RabbitTemplate rabbitTemplate;

    public StripeService(@Value("${webhook.secret}") String endpointSecret, RabbitTemplate rabbitTemplate) {    
        this.endpointSecret = endpointSecret;
        this.rabbitTemplate = rabbitTemplate;
    }

    public ResponseEntity<Void> handleWebhook(String payload, String sigHeader) {
        
        if (sigHeader == null) {
            return ResponseEntity.status(400).build();
        }

        Event event = null;

        try {
            event = ApiResource.GSON.fromJson(payload, Event.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Payload inválido do Stripe: {}", e.getMessage());
            return ResponseEntity.status(400).build();
        }

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

       
        String eventJson = event.toJson();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "stripe.event." + event.getType(), eventJson);
        logger.info("Evento publicado na fila RabbitMQ: {}", event.getType());

        return ResponseEntity.status(200).build();
    }
}