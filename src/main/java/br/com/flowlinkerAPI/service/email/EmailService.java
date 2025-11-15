package br.com.flowlinkerAPI.service.email;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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
    String html = loadTemplate("static/welcome.html")
        .replace("{{username}}", username)
        .replace("{{password}}", password);

        String text = "Bem vindo a Flowlinker! Seu usuário é " + username + " e sua senha temporária é " + password;
        
        smtp2GoClient.sendEmail(from, fromName, toEmail, "Bem vindo a Flowlinker", html, text);
       
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Redefinição de senha - FlowLinker";
        String text = "Para redefinir sua senha, acesse: " + resetLink + " (válido por tempo limitado).";
        String html = "<p>Você solicitou a redefinição de senha.</p>"
            + "<p><a href=\"" + resetLink + "\">Clique aqui para redefinir</a></p>"
            + "<p>Se você não solicitou, ignore este e-mail.</p>";
        smtp2GoClient.sendEmail(from, fromName, toEmail, subject, html, text);
    }

    private String loadTemplate(String templatePath) {
        try {
            var resource = new ClassPathResource(templatePath);
            try (var in = resource.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new WelcomeEmailNotSendException("Error loading template: " + templatePath);
        }
    }
    }
