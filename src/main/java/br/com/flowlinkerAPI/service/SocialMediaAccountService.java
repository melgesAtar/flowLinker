package br.com.flowlinkerAPI.service;

import java.util.List;
import br.com.flowlinkerAPI.model.SocialMediaAccount;
import br.com.flowlinkerAPI.repository.SocialMediaAccountRepository;    
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import br.com.flowlinkerAPI.dto.desktop.SocialMediaAccountResponse;
import br.com.flowlinkerAPI.dto.desktop.SocialCookieDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;


@Service
public class SocialMediaAccountService {

    private final SocialMediaAccountRepository socialMediaAccountRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public SocialMediaAccountService(SocialMediaAccountRepository socialMediaAccountRepository) {
        this.socialMediaAccountRepository = socialMediaAccountRepository;
    }

    public List<SocialMediaAccountResponse> listAccountsForPlatform(String platform, Long customerId) {
        var p = SocialMediaAccount.SocialMediaPlatform.valueOf(platform.toUpperCase());
        var rows = socialMediaAccountRepository.findAllByCustomerIdAndPlatformAndStatusNot(
            customerId,
            p,
            SocialMediaAccount.SocialMediaAccountStatus.DELETED
        );
        var out = new ArrayList<SocialMediaAccountResponse>();
        for (var a : rows) {
            var dto = new SocialMediaAccountResponse();
            dto.id = a.getId();
            dto.platform = a.getPlatform().name();
            dto.username = a.getUsername();
            dto.nomePerfil = a.getName();
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


    private String mapStatusToPt(SocialMediaAccount.SocialMediaAccountStatus s) {
        return switch (s) {
            case ACTIVE    -> "ATIVO";
            case INACTIVE  -> "INATIVO";
            case BLOCKED   -> "INATIVO"; 
            case DELETED   -> "DELETADO";
            case SUSPENDED -> "SUSPENSO";
        };
    }
}
