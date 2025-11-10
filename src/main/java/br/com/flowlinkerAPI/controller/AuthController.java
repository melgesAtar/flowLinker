package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.service.UserService;
import br.com.flowlinkerAPI.service.PasswordResetService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.flowlinkerAPI.dto.auth.ForgotPasswordRequestDTO;
import br.com.flowlinkerAPI.dto.auth.ResetPasswordRequestDTO;


@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Autenticação e redefinição de senha")
public class AuthController {   

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    private final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @Operation(
        summary = "Login",
        description = "Realiza a autenticação. Para `type=web`, o JWT é gravado em cookie httpOnly `jwtToken` e retorna 204. Para `type=device`, retorna o token no corpo."
    )
    @ApiResponse(responseCode = "200", description = "Autenticado (fluxo device)", content = @Content(mediaType = "text/plain"))
    @ApiResponse(responseCode = "204", description = "Autenticado (fluxo web, cookie definido)")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @PostMapping("/login")
    public ResponseEntity<?> login(
                                   @Parameter(description = "E-mail do usuário") @RequestParam String username, 
                                   @Parameter(description = "Senha") @RequestParam String password, 
                                   @Parameter(description = "`web` ou `device`") @RequestParam String type,
                                   @Parameter(description = "Fingerprint do device (obrigatório para type=device)") @RequestParam(required = false) String fingerprint,
                                   @Parameter(description = "Identificador do device") @RequestParam(required = false) String deviceId,
                                   @Parameter(description = "Hash de hardware") @RequestParam(required = false) String hwHash,
                                   @Parameter(description = "OS Name") @RequestParam(required = false) String osName,
                                   @Parameter(description = "OS Version") @RequestParam(required = false) String osVersion,
                                   @Parameter(description = "Arquitetura") @RequestParam(required = false) String arch,
                                   @Parameter(description = "Hostname") @RequestParam(required = false) String hostname,
                                   @Parameter(description = "Versão do app") @RequestParam(required = false) String appVersion,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        logger.info("Login request received for username: {}", username);
        String token = userService.loginAndGenerateToken(
            username, password, type, fingerprint, deviceId, hwHash,
            osName, osVersion, arch, hostname, appVersion,
            request, response
        );
        if("web".equals(type)) {
            return ResponseEntity.status(204).build();
        } else {
            return ResponseEntity.ok(token);
        }
    }

    @Operation(
        summary = "Solicitar redefinição de senha",
        description = "Envia um e-mail com link contendo JWT para redefinição de senha. Não revela se o e-mail existe."
    )
    @ApiResponse(responseCode = "204", description = "E-mail enviado (ou operação silenciada)")
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequestDTO body) {
        passwordResetService.sendResetEmail(body.getEmail(), body.getRedirectBaseUrl());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Redefinir senha",
        description = "Valida o token (JWT + Redis) e altera a senha do usuário."
    )
    @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso")
    @ApiResponse(responseCode = "400", description = "Token inválido ou expirado")
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequestDTO body) {
        passwordResetService.resetPassword(body.getToken(), body.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
