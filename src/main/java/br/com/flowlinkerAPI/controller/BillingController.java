package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.config.security.CurrentRequest;
import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.service.CustomerService;
import com.stripe.exception.StripeException;
import com.stripe.model.billingportal.Session;
import com.stripe.param.billingportal.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final CustomerService customerService;
    private final CurrentRequest currentRequest;
    private final Logger logger = LoggerFactory.getLogger(BillingController.class);

    @Value("${stripe.portal.return-url:}")
    private String defaultReturnUrl;

    public BillingController(CustomerService customerService, CurrentRequest currentRequest) {
        this.customerService = customerService;
        this.currentRequest = currentRequest;
    }

    @PostMapping("/portal")
    public ResponseEntity<Map<String, String>> createPortalSession(@RequestHeader(value = "Origin", required = false) String origin,
                                                                   @RequestParam(value = "returnUrl", required = false) String returnUrl) {
        Long customerId = currentRequest.getCustomerId();
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Customer customer = customerService.findById(customerId);
        String stripeCustomerId = customer.getStripeCustomerId();
        if (stripeCustomerId == null || stripeCustomerId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", "NO_STRIPE_CUSTOMER",
                "message", "Cliente ainda não vinculado ao Stripe."
            ));
        }

        String finalReturn = pickReturnUrl(returnUrl, origin, defaultReturnUrl);
        try {
            SessionCreateParams.Builder b = SessionCreateParams.builder()
                .setCustomer(stripeCustomerId);
            if (finalReturn != null && !finalReturn.isBlank()) {
                b.setReturnUrl(finalReturn);
            }
            SessionCreateParams params = b.build();

            Session session = Session.create(params);
            logger.info("Stripe Billing Portal session criada para customerId={} stripeCustomerId={}", String.valueOf(customerId), stripeCustomerId);
            return ResponseEntity.ok(Map.of("url", session.getUrl()));
        } catch (StripeException e) {
            logger.error("Erro ao criar sessão do Billing Portal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "code", "STRIPE_ERROR",
                "message", "Falha ao criar sessão do portal Stripe"
            ));
        }
    }

    private String pickReturnUrl(String explicit, String origin, String configured) {
        if (explicit != null && !explicit.isBlank()) return explicit;
        if (configured != null && !configured.isBlank()) return configured;
        if (origin != null && !origin.isBlank()) {
            if (origin.endsWith("/")) return origin + "app/billing";
            return origin + "/app/billing";
        }
        return null;
    }
}


