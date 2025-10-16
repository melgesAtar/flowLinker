package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stripe")
public class StripeCrontroller {

    private final StripeService stripeService;

    public StripeCrontroller(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        return stripeService.handleWebhook(payload, sigHeader);
    }
}