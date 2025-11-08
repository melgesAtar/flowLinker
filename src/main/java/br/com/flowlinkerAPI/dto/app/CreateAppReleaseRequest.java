package br.com.flowlinkerAPI.dto.app;

public class CreateAppReleaseRequest {
    public String platform;         // win, mac, linux
    public String arch;             // x64, arm64
    public String version;          // ex: 0.0.2
    public String minimumVersion;   // ex: 0.0.1 (opcional)
    public String downloadUrl;      // URL pública do MSI/instalador
    public String sha256;           // hash SHA-256 do arquivo
    public Long fileSize;           // tamanho em bytes (opcional)
    public Boolean forceUpdate;     // default false
    public String releaseNotes;     // changelog (opcional)
    public Boolean isActive;        // default true
}


