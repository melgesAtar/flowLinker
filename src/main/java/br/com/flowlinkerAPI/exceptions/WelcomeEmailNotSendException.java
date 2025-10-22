package br.com.flowlinkerAPI.exceptions;


public class WelcomeEmailNotSendException extends RuntimeException {
   
    public WelcomeEmailNotSendException(String message) {
        super(message);
    }
}
