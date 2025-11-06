package br.com.flowlinkerAPI.config.filter;

import br.com.flowlinkerAPI.config.security.CurrentRequest;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.model.DeviceStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class InactiveDeviceFilter extends OncePerRequestFilter {

    private final CurrentRequest currentRequest;

    public InactiveDeviceFilter(CurrentRequest currentRequest) {
        this.currentRequest = currentRequest;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Device device = currentRequest.getDevice();
        if (device != null && device.getStatus() == DeviceStatus.INACTIVE) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"DEVICE_INACTIVE\",\"message\":\"Este dispositivo está inativo. Reative para continuar.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}


