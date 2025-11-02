package br.com.flowlinkerAPI.dto.desktop;

import java.time.LocalDateTime;

public class SocialMediaAccountResponse {
    public Long id;
    public String platform;              
    public String username;              
    public String perfilName;            
    public String status;               
    public boolean hasCookies;          
    public LocalDateTime cookiesUpdatedAt;
    public LocalDateTime cookiesExpiresAt;
}