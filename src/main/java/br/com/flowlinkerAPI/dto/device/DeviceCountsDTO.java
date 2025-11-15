package br.com.flowlinkerAPI.dto.device;

public record DeviceCountsDTO(
        Long customerId,
        int total,
        int active,
        int inactive,
        int allowed
) {}

