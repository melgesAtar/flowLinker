package br.com.flowlinkerAPI.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.repository.DeviceRepository;

@Component
@RequiredArgsConstructor
public class CurrentRequest {

    private final DeviceRepository deviceRepository;

    public CurrentUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser cu) {
            return cu;
        }
        return null;
    }

    public Long getCustomerId() {
        CurrentUser cu = get();
        return cu != null ? cu.customerId() : null;
    }

    public String getUsername() {
        CurrentUser cu = get();
        return cu != null ? cu.username() : null;
    }

    public String getDeviceFingerprint() {
        CurrentUser cu = get();
        return cu != null ? cu.deviceFingerprint() : null;
    }

    public String getRole() {
        CurrentUser cu = get();
        return cu != null ? cu.role() : null;
    }

    public boolean isAdmin() {
        CurrentUser cu = get();
        return cu != null && cu.isAdmin();
    }

    public boolean isDevice() {
        return getDeviceFingerprint() != null;
    }

    public Device getDevice() {
        Long customerId = getCustomerId();
        String fp = getDeviceFingerprint();
        if (customerId == null || fp == null) return null;
        return deviceRepository.findByCustomerIdAndFingerprint(customerId, fp).orElse(null);
    }
}


