package br.com.flowlinkerAPI.exceptions;

public class DeviceChangedException extends RuntimeException {
    public DeviceChangedException(String message) {
        super(message);
    }
}
