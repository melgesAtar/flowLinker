package br.com.flowlinkerAPI.dto.desktop;

import lombok.Data;

@Data
public class SocialMediaAccountCreateRequest {
    public String platform;  
    public String username;
    public String profileName;
}
