package br.com.flowlinkerAPI.config;

import br.com.flowlinkerAPI.model.User;
import br.com.flowlinkerAPI.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.enabled:false}")
    private boolean enabled;

    @Value("${admin.default.email:}")
    private String email;

    @Value("${admin.default.password:}")
    private String password;

    @Value("${admin.default.force-reset:false}")
    private boolean forceReset;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Admin default bootstrap habilitado, mas email ou senha não configurados.");
            return;
        }

        userRepository.findByUsername(email)
                .ifPresentOrElse(existing -> updateExistingAdmin(existing),
                        () -> createAdmin());
    }

    private void createAdmin() {
        User admin = new User();
        admin.setUsername(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);
        log.info("Usuário admin '{}' criado via bootstrap.", email);
    }

    private void updateExistingAdmin(User existing) {
        boolean updated = false;
        if (existing.getRole() != User.Role.ADMIN) {
            existing.setRole(User.Role.ADMIN);
            updated = true;
        }
        if (forceReset && !passwordEncoder.matches(password, existing.getPassword())) {
            existing.setPassword(passwordEncoder.encode(password));
            updated = true;
        }
        if (updated) {
            userRepository.save(existing);
            log.info("Usuário admin '{}' atualizado via bootstrap.", email);
        } else {
            log.info("Usuário admin '{}' já existe e está configurado.", email);
        }
    }
}

