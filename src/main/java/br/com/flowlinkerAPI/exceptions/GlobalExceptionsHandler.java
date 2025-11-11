package br.com.flowlinkerAPI.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import br.com.flowlinkerAPI.exceptions.dto.CustomerNotFoundDTO;
import br.com.flowlinkerAPI.exceptions.dto.WelcomeEmailNotSendExceptionDTO;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionsHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionsHandler.class);

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<CustomerNotFoundDTO> handleCustomerNotFound(CustomerNotFoundException e) {
        logger.warn("Customer not found: {}", e.getMessage());
        return ResponseEntity.status(404)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new CustomerNotFoundDTO(e.getMessage()));
    }


    @ExceptionHandler(WelcomeEmailNotSendException.class)
    public ResponseEntity<WelcomeEmailNotSendExceptionDTO> handleWelcomeEmailNotSendException(WelcomeEmailNotSendException e) {
        logger.error("Welcome email not sent: {}", e.getMessage());
        return ResponseEntity.status(500)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new WelcomeEmailNotSendExceptionDTO(e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException e) {
        logger.warn("Bad credentials: {}", e.getMessage());
        return ResponseEntity.status(401)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
            "code", "AUTH_INVALID_CREDENTIALS",
            "message", "Invalid credentials"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.status(400)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
            "code", "BAD_REQUEST",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(LimitDevicesException.class)
    public ResponseEntity<Map<String, String>> handleLimitDevices(LimitDevicesException e) {
        logger.warn("Device limit reached: {}", e.getMessage());
        return ResponseEntity.status(403)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
            "code", "DEVICE_LIMIT_REACHED",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(DeviceInactiveException.class)
    public ResponseEntity<Map<String, String>> handleDeviceInactive(DeviceInactiveException e) {
        logger.warn("Device inactive: {}", e.getMessage());
        return ResponseEntity.status(403)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
            "code", "DEVICE_INACTIVE",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(DeviceChangedException.class)
    public ResponseEntity<Map<String, String>> handleDeviceChanged(DeviceChangedException e) {
        logger.warn("Device changed: {}", e.getMessage());
        return ResponseEntity.status(403)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
            "code", "DEVICE_CHANGED",
            "message", e.getMessage()
        ));
    }


}
