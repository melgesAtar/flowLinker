package br.com.flowlinkerAPI.config.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class CryptoKeyInitializer {

    @Value("${encryption.secret}")
    private String encryptionSecret;

    @PostConstruct
    public void init() {
        EncryptionUtils.init(encryptionSecret);
    }
}


