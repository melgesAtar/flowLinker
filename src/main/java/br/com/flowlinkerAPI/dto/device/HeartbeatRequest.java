package br.com.flowlinkerAPI.dto.device;

public class HeartbeatRequest {
    public String appVersion;
    public String ip; // opcional; se ausente, usa IP do request
    public String status; // opcional: ACTIVE/INACTIVE, etc.
}


