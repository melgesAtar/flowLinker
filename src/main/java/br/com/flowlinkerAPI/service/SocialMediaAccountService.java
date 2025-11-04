package br.com.flowlinkerAPI.service;

import java.util.List;
import br.com.flowlinkerAPI.model.SocialMediaAccount;
import br.com.flowlinkerAPI.repository.SocialMediaAccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountResponse;
import br.com.flowlinkerAPI.dto.desktop.SocialCookieDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountCreateRequest;
import org.springframework.web.server.ResponseStatusException;
import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.exceptions.CustomerNotFoundException;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountStatusPatch;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.flowlinkerAPI.config.security.CurrentRequest;

@Service
public class SocialMediaAccountService {

    private final SocialMediaAccountRepository socialMediaAccountRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final CustomerService customerService;
    private static final Logger logger = LoggerFactory.getLogger(SocialMediaAccountService.class);
    private final CurrentRequest currentRequest;

    public SocialMediaAccountService(SocialMediaAccountRepository socialMediaAccountRepository, CustomerService customerService, CurrentRequest currentRequest) {
        this.customerService = customerService;
        this.socialMediaAccountRepository = socialMediaAccountRepository;
        this.currentRequest = currentRequest;
    }

    public SocialMediaAccountResponse createAccount(SocialMediaAccountCreateRequest request, Long customerId) {
        logger.info("Criando conta social platform={} username={} customerId={}",
            (request != null ? request.platform : null), (request != null ? request.username : null), String.valueOf(customerId));
        if(request == null || request.platform == null || request.username == null || request.profileName == null || request.profileName.isBlank()) {
            throw new IllegalArgumentException("Invalid request");
        }

        var platform = parsePlatform(request.platform);
        boolean exists = socialMediaAccountRepository.existsByCustomerIdAndPlatformAndUsernameIgnoreCase(customerId, platform, request.username);
        if(exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conta já existe");
        }
        
        Customer customer = customerService.findById(customerId);
        if(customer == null) {
            throw new CustomerNotFoundException("Customer not found");
        }

        var account = new SocialMediaAccount();
        account.setCustomer(customer);
        account.setPlatform(platform);
        account.setUsername(request.username);
        account.setPassword(request.password);
        account.setName(request.profileName);
        account.setStatus(SocialMediaAccount.SocialMediaAccountStatus.ACTIVE);

        // Relaciona com o device do contexto (se for requisição de device)
        var device = currentRequest.getDevice();
        if (device != null) {
            account.setDevice(device);
        }

        var saved = socialMediaAccountRepository.save(account);
        logger.info("Conta social criada id={} platform={} username={} customerId={}", String.valueOf(saved.getId()), saved.getPlatform(), saved.getUsername(), String.valueOf(customerId));
        return toResponse(saved);
    }

    public SocialMediaAccountResponse updateAccount(Long id, Long customerId, SocialMediaAccountUpdateRequest body) {
        logger.info("Atualizando conta social id={} customerId={}", String.valueOf(id), String.valueOf(customerId));
        var acc = socialMediaAccountRepository.findById(id).orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        
        if(acc.getCustomer() == null || !acc.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de outro cliente");
        }

        if(body == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Body invalido");
        }

        var newPlatform = parsePlatform(body.platform);
        var newUsername = body.username;

        if(body.platform != null){
            try{
                newPlatform = parsePlatform(body.platform);
            }catch(IllegalArgumentException e){
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Platform invalida: " + body.platform);
            }
        }

        if (body.username != null) {
            if (body.username.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Username não pode ser vazio");
            }
            newUsername = body.username;
        }
    
        if ((body.platform != null) || (body.username != null)) {
            boolean dup = socialMediaAccountRepository
                .existsByCustomerIdAndPlatformAndUsernameIgnoreCaseAndIdNot(customerId, newPlatform, newUsername, id);
            if (dup) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Conta já existe para este cliente");
            }
        }

        if (body.platform != null) acc.setPlatform(newPlatform);
        if (body.username != null) acc.setUsername(newUsername);
        if (body.nomePerfil != null) acc.setName(body.nomePerfil);
        if (body.password != null && !body.password.isBlank()) acc.setPassword(body.password);
    
        acc.setUpdatedAt(java.time.LocalDateTime.now());
    
        var saved = socialMediaAccountRepository.save(acc);
        logger.info("Conta social atualizada id={} platform={} username={}", String.valueOf(saved.getId()), saved.getPlatform(), saved.getUsername());
        return toResponse(saved);

    }

    public void replaceCookies(Long id, Long customerId, List<SocialCookieDTO> cookies) {
        logger.info("Substituindo cookies id={} customerId={} qtdCookies={} (conteúdo não logado)", String.valueOf(id), String.valueOf(customerId), (cookies != null ? cookies.size() : 0));
        var acc = socialMediaAccountRepository.findById(id).orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        if (acc.getCustomer() == null || !acc.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de outro cliente");
        }
        if (cookies == null || cookies.isEmpty()) {
            throw new IllegalArgumentException("Cookies cannot be null or empty");
        }

        for (var c : cookies) {
            if (c.name == null || c.value == null || c.domain == null || c.path == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cookies com campos ausentes");
            }
            if (c.isSecure == null) c.isSecure = Boolean.TRUE;
            if (c.isHttpOnly == null) c.isHttpOnly = Boolean.TRUE;
            if (c.expiry != null) {
                long v = c.expiry;
                c.expiry = (v >= 1_000_000_000_000L) ? (v / 1000L) : v;
            }
        }

        Long minExpiry = cookies.stream()
        .map(c -> c.expiry)
        .filter(e -> e != null && e > 0)
        .min(Long::compareTo)
        .orElse(null);

  
        try {
            String json = mapper.writeValueAsString(cookies);
            // atualizar campos
            setCookies(acc, json, minExpiry);
            socialMediaAccountRepository.save(acc);
        } catch (Exception e) {
            logger.error("Erro ao salvar cookies para id={}: {}", String.valueOf(id), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao salvar cookies");
        }
    
    }

    public SocialMediaAccountResponse updateStatus(Long id, Long customerId, SocialMediaAccountStatusPatch body) {
        logger.info("Atualizando status da conta id={} customerId={} novoStatus={} ", String.valueOf(id), String.valueOf(customerId), (body != null ? body.status : null));
        if(body == null || body.status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status Invalido");
        }

        var acc = socialMediaAccountRepository.findById(id).orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if(acc.getCustomer().getId() != customerId){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de outro cliente");
        }

        var newStatus = parseStatusPt(body.status);

        if(newStatus == acc.getStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status já atualizado");
        }
        acc.setStatus(newStatus);
        var saved = socialMediaAccountRepository.save(acc);
        logger.info("Status atualizado id={} statusNovo={}", String.valueOf(saved.getId()), saved.getStatus());
        return toResponse(saved);
    }

    public void softDelete(Long id, Long customerId) {
        logger.info("Soft delete conta social id={} customerId={}", String.valueOf(id), String.valueOf(customerId));
        var acc = socialMediaAccountRepository.findById(id).orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        if(acc.getCustomer().getId() != customerId){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de outro cliente");
        }
        acc.setStatus(SocialMediaAccount.SocialMediaAccountStatus.DELETED);
        socialMediaAccountRepository.save(acc);
    }

    public List<SocialMediaAccountResponse> listAccountsForPlatform(String platform, Long customerId) {
        logger.info("Listando contas platform={} customerId={}", platform, customerId);
        var p = SocialMediaAccount.SocialMediaPlatform.valueOf(platform.toUpperCase());
        var rows = socialMediaAccountRepository.findAllByCustomerIdAndPlatformAndStatusNot(
            customerId,
            p,
            SocialMediaAccount.SocialMediaAccountStatus.DELETED
        );
        logger.info("Encontradas {} contas para platform={} customerId={}", rows.size(), platform, String.valueOf(customerId));
        var out = new ArrayList<SocialMediaAccountResponse>();
        for (var a : rows) {
            var dto = new SocialMediaAccountResponse();
            dto.id = a.getId();
            dto.platform = a.getPlatform().name();
            dto.username = a.getUsername();
            dto.password = a.getPassword();
            dto.perfilName = a.getName();
            dto.status = mapStatusToPt(a.getStatus());
            dto.hasCookies = a.getCookiesJson() != null && !a.getCookiesJson().isBlank();
            dto.cookiesUpdatedAt = a.getUpdatedAt();     
            dto.cookiesExpiresAt = a.getCoookiesExpiry(); 
            out.add(dto);
        }
        return out;
    }


    public List<SocialCookieDTO> getCookies(Long id, Long customerId) {
        var acc = socialMediaAccountRepository.findById(id).orElseThrow();
        if (acc.getCustomer() == null || !acc.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Forbidden");
        }
        if (acc.getCookiesJson() == null || acc.getCookiesJson().isBlank()) {
            return List.of();
        }
        try {
            
            List<Map<String, Object>> raw = mapper.readValue(acc.getCookiesJson(), new TypeReference<>() {});
            var out = new ArrayList<SocialCookieDTO>();
            for (var c : raw) {
                var dto = new SocialCookieDTO();
                dto.name = (String) c.get("name");
                dto.value = (String) c.get("value");
                dto.domain = (String) c.get("domain");
                dto.path = (String) c.get("path");
                dto.isSecure = (Boolean) c.getOrDefault("isSecure", Boolean.TRUE);
                dto.isHttpOnly = (Boolean) c.getOrDefault("isHttpOnly", Boolean.TRUE);

              
                Long expiry = null;
                Object expiryObj = c.get("expiry");
                if (expiryObj instanceof Number n) {
                    long v = n.longValue();
                   
                    expiry = (v >= 1_000_000_000_000L) ? (v / 1000L) : v;
                }
                dto.expiry = expiry;

                out.add(dto);
            }
            return out;
        } catch (Exception ex) {
            return List.of();
        }
    }


    
    //aux
    
    private String mapStatusToPt(SocialMediaAccount.SocialMediaAccountStatus s) {
        return switch (s) {
            case ACTIVE    -> "ATIVO";
            case INACTIVE  -> "INATIVO";
            case BLOCKED   -> "INATIVO"; 
            case DELETED   -> "DELETADO";
            case SUSPENDED -> "SUSPENSO";
        };
    }
    
    private SocialMediaAccount.SocialMediaPlatform parsePlatform(String platform) {
        try{
            return SocialMediaAccount.SocialMediaPlatform.valueOf(platform.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid platform: " + platform);
        }
        
    }

    public SocialMediaAccount.SocialMediaAccountStatus parseStatusPt(String status) {
        return switch (status) {
            case "ATIVO" -> SocialMediaAccount.SocialMediaAccountStatus.ACTIVE;
            case "INATIVO" -> SocialMediaAccount.SocialMediaAccountStatus.INACTIVE;
            case "DELETADO" -> SocialMediaAccount.SocialMediaAccountStatus.DELETED;
            case "SUSPENSO" -> SocialMediaAccount.SocialMediaAccountStatus.SUSPENDED;
            case "BLOQUEADO" -> SocialMediaAccount.SocialMediaAccountStatus.BLOCKED;
            default -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "status invalido: " + status);
        };
    }

    private SocialMediaAccountResponse toResponse(SocialMediaAccount a) {
        var dto = new SocialMediaAccountResponse();
        dto.id = a.getId();
        dto.platform = a.getPlatform().name();
        dto.username = a.getUsername();
        dto.perfilName = a.getName();
        dto.status = mapStatusToPt(a.getStatus());
        dto.hasCookies = a.getCookiesJson() != null && !a.getCookiesJson().isBlank();
        dto.cookiesUpdatedAt = a.getUpdatedAt();
        dto.cookiesExpiresAt = a.getCoookiesExpiry();
        return dto;
    }

    
    private void setCookies(SocialMediaAccount acc, String json, Long minExpiry) {
        acc.setCookiesJson(json);
        acc.setUpdatedAt(LocalDateTime.now());

        if (minExpiry != null) {
            LocalDateTime expiry = LocalDateTime.ofEpochSecond(minExpiry, 0, ZoneOffset.UTC);
            if (expiry.isAfter(LocalDateTime.now())) {
                acc.setCoookiesExpiry(expiry);
            }
        } else {
            acc.setCoookiesExpiry(null);
        }
    }
}
