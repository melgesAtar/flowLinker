package br.com.flowlinkerAPI.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Autenticação e sessão")
public class AuthSessionController {
    
    @Operation(summary = "Verificar sessão", description = "Retorna 204 se autenticado, 401 caso contrário.")
    @GetMapping("/me")
    public ResponseEntity<Void> checkSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.noContent().build(); 
        }
        return ResponseEntity.status(401).build();
    }
}
