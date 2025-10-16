package br.com.flowlinkerAPI.config;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class StripeConfig {

    private final String apiKey;

    public StripeConfig(@Value("${stripe.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}


