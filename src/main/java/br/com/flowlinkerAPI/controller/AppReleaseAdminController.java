package br.com.flowlinkerAPI.controller;
import br.com.flowlinkerAPI.model.AppRelease;
import br.com.flowlinkerAPI.repository.AppReleaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/admin/releases")
public class AppReleaseAdminController {

    private final AppReleaseRepository appReleaseRepository;
    @Value("${releases.quick.token:}")
    private String quickToken;
    @Value("${cdn.publicBaseUrl:}")
    private String publicBaseUrl;
    @Value("${cdn.s3.bucket:}")
    private String s3Bucket;
    @Value("${cdn.s3.region:}")
    private String s3Region;

    public AppReleaseAdminController(AppReleaseRepository appReleaseRepository) {
        this.appReleaseRepository = appReleaseRepository;
    }




    
    @PostMapping(value = "/quick/upload")
    public ResponseEntity<?> quickUpload(@RequestHeader(value = "X-Release-Token", required = false) String token,
                                         @RequestBody QuickReleaseRequest body) {
        if (quickToken == null || quickToken.isBlank() || token == null || !quickToken.equals(token)) {
            return ResponseEntity.status(403).body(Map.of(
                    "code", "INVALID_TOKEN",
                    "message", "Token inválido para criação rápida de release"
            ));
        }
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "BODY_REQUIRED",
                    "message", "Payload JSON é obrigatório"
            ));
        }
        if (body.platform == null || body.platform.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "PLATFORM_REQUIRED",
                    "message", "Campo platform é obrigatório"
            ));
        }
        if (body.arch == null || body.arch.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "ARCH_REQUIRED",
                    "message", "Campo arch é obrigatório"
            ));
        }
        if (body.version == null || body.version.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "VERSION_REQUIRED",
                    "message", "Campo version é obrigatório"
            ));
        }
        if (body.sha256 == null || body.sha256.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "SHA256_REQUIRED",
                    "message", "Campo sha256 é obrigatório"
            ));
        }
        if (body.fileName == null || body.fileName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "FILENAME_REQUIRED",
                    "message", "Campo fileName é obrigatório"
            ));
        }
        if (body.fileSize == null || body.fileSize < 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "FILESIZE_REQUIRED",
                    "message", "Campo fileSize deve ser informado e não pode ser negativo"
            ));
        }

        AppRelease.Platform platform = parsePlatform(body.platform);
        AppRelease.Arch arch = parseArch(body.arch);
        try {
            String safeVersion = body.version.replaceAll("[^0-9A-Za-z\\.-]", "_");
            String baseFolder = "flowlinker-v" + safeVersion + "/";
            String filename = sanitizeFileName(body.fileName, safeVersion);
            filename = filename.replaceAll("[^0-9A-Za-z._-]", "_");
            String key = baseFolder + filename;

            System.out.printf("Release recebida: arquivo='%s', pastaS3='%s'%n", filename, baseFolder);

            String sha256Hex = body.sha256.trim();
            long sizeBytes = Objects.requireNonNullElse(body.fileSize, 0L);

            var existing = appReleaseRepository.findFirstByPlatformAndArchAndIsActiveOrderByCreatedAtDesc(platform, arch, true)
                    .filter(r -> r.getVersion().equals(body.version))
                    .orElse(null);

            AppRelease r = (existing != null) ? existing : new AppRelease();
            r.setPlatform(platform);
            r.setArch(arch);
            r.setVersion(body.version);
            r.setMinimumVersion(emptyToNull(body.minimumVersion));
            String downloadUrl = resolvePublicUrl(key);
            if (body.downloadUrlOverride != null && !body.downloadUrlOverride.isBlank()) {
                downloadUrl = body.downloadUrlOverride.trim();
            }
            r.setDownloadUrl(downloadUrl);
            r.setS3Key(key);
            r.setSha256(sha256Hex);
            r.setFileSize(sizeBytes);
            r.setForceUpdate(body.forceUpdate != null ? body.forceUpdate : false);
            r.setReleaseNotes(emptyToNull(body.releaseNotes));
            r.setIsActive(body.isActive != null ? body.isActive : true);

            AppRelease saved = appReleaseRepository.save(r);
            return ResponseEntity.status(existing == null ? 201 : 200).body(Map.of(
                    "id", saved.getId(),
                    "version", saved.getVersion(),
                    "platform", saved.getPlatform().name(),
                    "arch", saved.getArch().name(),
                    "downloadUrl", saved.getDownloadUrl(),
                    "sha256", saved.getSha256(),
                    "fileSize", saved.getFileSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", "PROCESSING_FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    private String sanitizeFileName(String original, String safeVersion) {
        if (original == null || original.isBlank()) {
            return "Flowlinker-" + safeVersion + ".msi";
        }
        String normalized = original.replaceAll("^.*[\\\\/]", "");
        return normalized.isBlank() ? "Flowlinker-" + safeVersion + ".msi" : normalized;
    }

    private String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    private AppRelease.Platform parsePlatform(String p) {
        if (p == null) throw new IllegalArgumentException("platform required");
        switch (p.toLowerCase()) {
            case "win":
            case "windows":
                return AppRelease.Platform.WIN;
            case "mac":
            case "darwin":
            case "osx":
            case "macos":
                return AppRelease.Platform.MAC;
            case "linux":
                return AppRelease.Platform.LINUX;
            default:
                throw new IllegalArgumentException("unsupported platform: " + p);
        }
    }

    private AppRelease.Arch parseArch(String a) {
        if (a == null) throw new IllegalArgumentException("arch required");
        switch (a.toLowerCase()) {
            case "x64":
            case "amd64":
                return AppRelease.Arch.X64;
            case "arm64":
            case "aarch64":
                return AppRelease.Arch.ARM64;
            default:
                throw new IllegalArgumentException("unsupported arch: " + a);
        }
    }

    private String resolvePublicUrl(String key) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            return base + "/" + key;
        }
        if (s3Bucket != null && !s3Bucket.isBlank() && s3Region != null && !s3Region.isBlank()) {
            return "https://" + s3Bucket + ".s3." + s3Region + ".amazonaws.com/" + key;
        }
        return key;
    }

    public static class QuickReleaseRequest {
        public String platform;
        public String arch;
        public String version;
        public String minimumVersion;
        public Boolean forceUpdate;
        public String releaseNotes;
        public Boolean isActive;
        public String fileName;
        public Long fileSize;
        public String sha256;
        public String downloadUrlOverride;
    }
}


