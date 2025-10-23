package br.com.flowlinkerAPI.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import br.com.flowlinkerAPI.exceptions.dto.CustomerNotFoundDTO;
import br.com.flowlinkerAPI.exceptions.dto.LimitDevicesExceptionDTO;
import br.com.flowlinkerAPI.exceptions.dto.WelcomeEmailNotSendExceptionDTO;


@RestControllerAdvice
public class GlobalExceptionsHandler {
    


    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<CustomerNotFoundDTO> handleCustomerNotFound(CustomerNotFoundException e) {
        return ResponseEntity.status(404).body(new CustomerNotFoundDTO(e.getMessage()));
    }

    @ExceptionHandler(LimitDevicesException.class)
    public ResponseEntity<LimitDevicesExceptionDTO> handleLimitDevicesException(LimitDevicesException e) {
        return ResponseEntity.status(400).body(new LimitDevicesExceptionDTO(e.getMessage()));
    }

    @ExceptionHandler(WelcomeEmailNotSendException.class)
    public ResponseEntity<WelcomeEmailNotSendExceptionDTO> handleWelcomeEmailNotSendException(WelcomeEmailNotSendException e) {
        return ResponseEntity.status(500).body(new WelcomeEmailNotSendExceptionDTO(e.getMessage()));
    }
}
