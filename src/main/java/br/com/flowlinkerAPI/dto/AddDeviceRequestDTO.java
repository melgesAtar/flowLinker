package br.com.flowlinkerAPI.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddDeviceRequestDTO {
    private String fingerprint;
    private String name;
    private Long customerId;
}
