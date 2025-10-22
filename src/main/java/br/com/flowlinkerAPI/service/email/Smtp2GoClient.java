package br.com.flowlinkerAPI.service.email;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import br.com.flowlinkerAPI.exceptions.WelcomeEmailNotSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Service
public class Smtp2GoClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${smtp2go.api.key}")
    private String apiKey;

    @Value("${smtp2go.api.url}")
    private String apiUrl;

    private final Logger logger = LoggerFactory.getLogger(Smtp2GoClient.class);

    public void  sendEmail(String from, String fromName, String toEmail, String subject, String htmlBody, String textBody){

        String url = apiUrl + "/email/send";
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", String.format("%s <%s>", fromName, from));
        payload.put("to", Collections.singletonList(toEmail));
        payload.put("subject", subject);

        if(textBody !=null && !textBody.isBlank()){
            payload.put("text_body", textBody);
        }
        payload.put("html_body", htmlBody);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Smtp2go-Api-Key", apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        
        if(!response.getStatusCode().is2xxSuccessful()){
          throw new WelcomeEmailNotSendException("Error sending welcome email: " + response.getBody());
        }
        logger.info("Email sent successfully to: {}", toEmail);
     }
}
