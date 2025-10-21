package br.com.flowlinkerAPI.service.email;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Service
public class Smtp2GoClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${smtp2go.api.key}")
    private String apiKey;

    @Value("${smtp2go.api.url}")
    private String apiUrl;

    public void  sendEmail(String from, String fromName, String toEmail, String subject, String htmlBody){

        String url = apiUrl + "/email/send";
        
    }
}
