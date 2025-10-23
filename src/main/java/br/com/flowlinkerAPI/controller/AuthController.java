package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
public class AuthController {   

    private final UserService userService;

    private final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, 
                                        @RequestParam String password, 
                                        @RequestParam String type,
                                        @RequestParam(required = false) String fingerprint) {
        try {
            String token = userService.loginAndGenerateToken(username, password, type, fingerprint);    
            logger.info("Token gerado para o usuário {} com tipo {} e fingerprint {}", username, type, fingerprint);
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
