package br.com.flowlinkerAPI.exceptions.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WelcomeEmailNotSendExceptionDTO {
    private String message;
    
}
