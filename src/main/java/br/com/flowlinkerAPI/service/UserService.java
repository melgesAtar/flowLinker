package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.User;
import br.com.flowlinkerAPI.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import org.slf4j.*;

@Service
public class UserService {

    
    private final UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createOrGetUserByEmail(String email) {

        return userRepository.findByUsername(email)
        .orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(email);
            String randomPassword = generateRandomPassword(12);
            logger.info("Generated password for user " + email + ": " + randomPassword);
            String hashedPassword = passwordEncoder.encode(randomPassword);
            newUser.setPassword(hashedPassword);
            return userRepository.save(newUser);
        });
    }


    public String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

}
