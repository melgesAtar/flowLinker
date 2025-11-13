package br.com.flowlinkerAPI.config.filter;

import br.com.flowlinkerAPI.config.security.CurrentRequest;
import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import org.springframework.lang.NonNull;

public class ActiveSubscriptionFilter extends OncePerRequestFilter {

    private final CurrentRequest currentRequest;
    private final CustomerRepository customerRepository;

    public ActiveSubscriptionFilter(CurrentRequest currentRequest, CustomerRepository customerRepository) {
        this.currentRequest = currentRequest;
        this.customerRepository = customerRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        Long customerId = currentRequest.getCustomerId();
        if (customerId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            deny(response, "SUBSCRIPTION_REQUIRED", "Assinatura necessária para acessar este recurso.");
            return;
        }

        Customer.SubscriptionStatus status = customer.getSubscriptionStatus();
        Instant now = Instant.now();
        Instant end = customer.getSubscriptionEndDate();

        boolean allowed = false;

        if (status == Customer.SubscriptionStatus.ACTIVE) {
            allowed = true;
        } else if (status == Customer.SubscriptionStatus.TRIALING) {
            allowed = end != null && !now.isAfter(end);
        } else if (status == Customer.SubscriptionStatus.CANCELED) {
            // Mantém acesso até a data de término do período vigente
            allowed = end != null && !now.isAfter(end);
        }

        if (!allowed) {
            String code = (status == Customer.SubscriptionStatus.TRIALING) ? "TRIAL_EXPIRED" : "SUBSCRIPTION_INACTIVE";
            String message = (status == Customer.SubscriptionStatus.TRIALING)
                    ? "Seu período de avaliação expirou. Renove para continuar."
                    : "Sua assinatura não está ativa. Renove para continuar.";
            deny(response, code, message);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void deny(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        return path.startsWith("/auth/login")
                || path.startsWith("/auth/password/")
                || path.startsWith("/stripe/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/admin/releases/quick/")
                || path.startsWith("/devices/limits")
                || "OPTIONS".equalsIgnoreCase(method);
    }
}


