package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.Cookie;


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
                                        @RequestParam(required = false) String fingerprint,
                                        HttpServletResponse response) {
        try {

            String token = userService.loginAndGenerateToken(username, password, type, fingerprint, response);   
            
            if(type.equals("web")) {
                return ResponseEntity.ok("Login sucessfully - Cookie set");
            }else{
                return ResponseEntity.ok(token);
            }

            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
