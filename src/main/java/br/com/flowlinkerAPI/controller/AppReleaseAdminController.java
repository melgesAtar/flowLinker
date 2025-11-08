package br.com.flowlinkerAPI.controller;
import br.com.flowlinkerAPI.model.AppRelease;
import br.com.flowlinkerAPI.repository.AppReleaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import br.com.flowlinkerAPI.service.CdnUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/releases")
public class AppReleaseAdminController {

    private final AppReleaseRepository appReleaseRepository;
    private final CdnUploadService cdnUploadService;
    @Value("${releases.quick.token:}")
    private String quickToken;

    public AppReleaseAdminController(AppReleaseRepository appReleaseRepository, CdnUploadService cdnUploadService) {
        this.appReleaseRepository = appReleaseRepository;
        this.cdnUploadService = cdnUploadService;
    }





    @PostMapping(value = "/quick/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> quickUpload(@RequestHeader(value = "X-Release-Token", required = false) String token,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam("platform") String platformStr,
                                         @RequestParam("arch") String archStr,
                                         @RequestParam("version") String version,
                                         @RequestParam(value = "minimumVersion", required = false) String minimumVersion,
                                         @RequestParam(value = "forceUpdate", required = false, defaultValue = "false") boolean forceUpdate,
                                         @RequestParam(value = "releaseNotes", required = false) String releaseNotes,
                                         @RequestParam(value = "isActive", required = false, defaultValue = "true") boolean isActive) {
        if (quickToken == null || quickToken.isBlank() || token == null || !quickToken.equals(token)) {
            return ResponseEntity.status(403).body(java.util.Map.of(
                    "code", "INVALID_TOKEN",
                    "message", "Token inválido para criação rápida de release"
            ));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "code", "FILE_REQUIRED",
                    "message", "Arquivo MSI é obrigatório"
            ));
        }
        AppRelease.Platform platform = parsePlatform(platformStr);
        AppRelease.Arch arch = parseArch(archStr);
        try {
            String safeVersion = version.replaceAll("[^0-9A-Za-z\\.-]", "_");
            String baseFolder = "flowlinker-v" + safeVersion + "/";
            String original = file.getOriginalFilename();
            String filename = (original != null ? original.replaceAll("^.*[\\\\/]", "") : "Flowlinker-" + safeVersion + ".msi");
            filename = filename.replaceAll("[^0-9A-Za-z._-]", "_");
            String key = baseFolder + filename;
            byte[] bytes = file.getBytes();
            var upload = cdnUploadService.uploadBytes(key, bytes, "application/octet-stream");

            var existing = appReleaseRepository.findFirstByPlatformAndArchAndIsActiveOrderByCreatedAtDesc(platform, arch, true)
                    .filter(r -> r.getVersion().equals(version))
                    .orElse(null);

            AppRelease r = (existing != null) ? existing : new AppRelease();
            r.setPlatform(platform);
            r.setArch(arch);
            r.setVersion(version);
            r.setMinimumVersion(emptyToNull(minimumVersion));
            // Persistimos uma URL estável (pode não ser pública), e usamos presigned URL no download
            r.setDownloadUrl(upload.url);
            r.setS3Key(upload.key);
            r.setSha256(upload.sha256Hex);
            r.setFileSize(upload.sizeBytes);
            r.setForceUpdate(forceUpdate);
            r.setReleaseNotes(emptyToNull(releaseNotes));
            r.setIsActive(isActive);

            AppRelease saved = appReleaseRepository.save(r);
            return ResponseEntity.status(existing == null ? 201 : 200).body(java.util.Map.of(
                    "id", saved.getId(),
                    "version", saved.getVersion(),
                    "platform", saved.getPlatform().name(),
                    "arch", saved.getArch().name(),
                    "downloadUrl", saved.getDownloadUrl(),
                    "sha256", saved.getSha256(),
                    "fileSize", saved.getFileSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                    "code", "UPLOAD_FAILED",
                    "message", e.getMessage()
            ));
        }
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
}


