package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> login(@RequestParam String username, 
                                   @RequestParam String password, 
                                   @RequestParam String type,
                                   @RequestParam(required = false) String fingerprint,
                                   @RequestParam(required = false) String deviceId,
                                   @RequestParam(required = false) String hwHash,
                                   @RequestParam(required = false) String osName,
                                   @RequestParam(required = false) String osVersion,
                                   @RequestParam(required = false) String arch,
                                   @RequestParam(required = false) String hostname,
                                   @RequestParam(required = false) String appVersion,
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
}
