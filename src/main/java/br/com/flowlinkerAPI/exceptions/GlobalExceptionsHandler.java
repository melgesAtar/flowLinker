package br.com.flowlinkerAPI.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import br.com.flowlinkerAPI.exceptions.dto.CustomerNotFoundDTO;
import br.com.flowlinkerAPI.exceptions.dto.WelcomeEmailNotSendExceptionDTO;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionsHandler {
    


    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<CustomerNotFoundDTO> handleCustomerNotFound(CustomerNotFoundException e) {
        return ResponseEntity.status(404).body(new CustomerNotFoundDTO(e.getMessage()));
    }


    @ExceptionHandler(WelcomeEmailNotSendException.class)
    public ResponseEntity<WelcomeEmailNotSendExceptionDTO> handleWelcomeEmailNotSendException(WelcomeEmailNotSendException e) {
        return ResponseEntity.status(500).body(new WelcomeEmailNotSendExceptionDTO(e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(401).body(Map.of(
            "code", "AUTH_INVALID_CREDENTIALS",
            "message", "Invalid credentials"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(400).body(Map.of(
            "code", "BAD_REQUEST",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(LimitDevicesException.class)
    public ResponseEntity<Map<String, String>> handleLimitDevices(LimitDevicesException e) {
        return ResponseEntity.status(403).body(Map.of(
            "code", "DEVICE_LIMIT_REACHED",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(DeviceInactiveException.class)
    public ResponseEntity<Map<String, String>> handleDeviceInactive(DeviceInactiveException e) {
        return ResponseEntity.status(403).body(Map.of(
            "code", "DEVICE_INACTIVE",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(DeviceChangedException.class)
    public ResponseEntity<Map<String, String>> handleDeviceChanged(DeviceChangedException e) {
        return ResponseEntity.status(403).body(Map.of(
            "code", "DEVICE_CHANGED",
            "message", e.getMessage()
        ));
    }


}
