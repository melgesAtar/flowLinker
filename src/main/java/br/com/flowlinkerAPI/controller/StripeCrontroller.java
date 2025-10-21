package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/stripe")
public class StripeCrontroller {

    private final StripeService stripeService;
    private final Logger logger = LoggerFactory.getLogger(StripeCrontroller.class);


    public StripeCrontroller(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        logger.info("Recebido webhook do stripe");
        return stripeService.handleWebhook(payload, sigHeader);
    }
}