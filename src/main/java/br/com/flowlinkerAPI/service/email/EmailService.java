package br.com.flowlinkerAPI.service.email;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import br.com.flowlinkerAPI.exceptions.WelcomeEmailNotSendException;

@Service
public class EmailService {
   
    private final Smtp2GoClient smtp2GoClient;
    @Value("${smtp2go.from.email}")
    private String from;
    @Value("${smtp2go.from.name}")
    private String fromName;

    public EmailService(Smtp2GoClient smtp2GoClient) {
        this.smtp2GoClient = smtp2GoClient;
    }

    public void sendWelcomeEmail(String toEmail, String username, String password) throws WelcomeEmailNotSendException {
    String html = loadTemplate("/static/welcome.html")
        .replace("{{username}}", username)
        .replace("{{password}}", password);

        String text = "Bem vindo a Flowlinker! Seu usuário é " + username + " e sua senha temporária é " + password;
        
        smtp2GoClient.sendEmail(from, fromName, toEmail, "Bem vindo a Flowlinker", html, text);
       
    }

    private String loadTemplate(String templatePath) {
        try {
            var res = new ClassPathResource(templatePath);
            byte[] bytes = Files.readAllBytes(res.getFile().toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error loading template: " + templatePath, e);
        }
    }
    }
