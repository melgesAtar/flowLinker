package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.model.AppRelease;
import br.com.flowlinkerAPI.repository.AppReleaseRepository;
import br.com.flowlinkerAPI.service.CdnUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/app/releases")
@Tag(name = "App Releases", description = "Endpoints de download de releases do aplicativo")
public class AppReleaseDownloadController {

    private final AppReleaseRepository appReleaseRepository;
    private final CdnUploadService cdnUploadService;

    public AppReleaseDownloadController(AppReleaseRepository appReleaseRepository, CdnUploadService cdnUploadService) {
        this.appReleaseRepository = appReleaseRepository;
        this.cdnUploadService = cdnUploadService;
    }

    @GetMapping("/latest")
    @Operation(
        summary = "Obter link da última versão ativa",
        description = "Retorna a URL de download da última release ativa para a plataforma/arquitetura informadas. Se houver s3Key, retorna URL pré-assinada (15 min).",
        responses = {
            @ApiResponse(responseCode = "200", description = "URL gerada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Release não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Release sem s3Key e sem downloadUrl", content = @Content)
        }
    )
    public ResponseEntity<?> latest(@RequestParam(required = false, defaultValue = "WIN") String platform,
                                    @RequestParam(required = false, defaultValue = "X64") String arch) {
        AppRelease.Platform p = parsePlatform(platform);
        AppRelease.Arch a = parseArch(arch);
        var releaseOpt = appReleaseRepository.findFirstByPlatformAndArchAndIsActiveOrderByCreatedAtDesc(p, a, true);
        if (releaseOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("code","RELEASE_NOT_FOUND","message","Release não encontrada para plataforma/arquitetura"));
        }
        AppRelease r = releaseOpt.get();
        if (r.getS3Key() == null || r.getS3Key().isBlank()) {
            if (r.getDownloadUrl() != null && !r.getDownloadUrl().isBlank()) {
                return ResponseEntity.ok(Map.of(
                    "url", r.getDownloadUrl(),
                    "version", r.getVersion(),
                    "expiresAt", (Object)null
                ));
            }
            return ResponseEntity.status(409).body(Map.of("code","MISSING_S3KEY","message","Release sem s3Key nem downloadUrl"));
        }
        String url = cdnUploadService.presignGetUrl(r.getS3Key(), Duration.ofMinutes(15));
        String expiresAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plus(Duration.ofMinutes(15)));
        return ResponseEntity.ok(Map.of(
            "url", url,
            "version", r.getVersion(),
            "expiresAt", expiresAt
        ));
    }

    @GetMapping("/download")
    @Operation(
        summary = "Obter link de download para uma versão específica",
        description = "Retorna a URL de download para a versão ativa especificada por plataforma/arquitetura/versão. Se houver s3Key, retorna URL pré-assinada (15 min).",
        responses = {
            @ApiResponse(responseCode = "200", description = "URL gerada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Release ou versão não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Release sem s3Key e sem downloadUrl", content = @Content)
        }
    )
    public ResponseEntity<?> download(@Parameter(description = "Plataforma: WIN | MAC | LINUX") @RequestParam String platform,
                                      @Parameter(description = "Arquitetura: X64 | ARM64") @RequestParam String arch,
                                      @Parameter(description = "Versão SemVer (ex.: 1.2.3)") @RequestParam String version) {
        AppRelease.Platform p = parsePlatform(platform);
        AppRelease.Arch a = parseArch(arch);
        var releaseOpt = appReleaseRepository.findFirstByPlatformAndArchAndIsActiveOrderByCreatedAtDesc(p, a, true);
        if (releaseOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("code","RELEASE_NOT_FOUND","message","Release não encontrada para plataforma/arquitetura"));
        }
        AppRelease r = releaseOpt.get();
        if (!version.equals(r.getVersion())) {
            if (r.getVersion() == null || !r.getVersion().equals(version)) {
                return ResponseEntity.status(404).body(Map.of("code","RELEASE_VERSION_NOT_FOUND","message","Versão informada não encontrada como ativa"));
            }
        }
        if (r.getS3Key() == null || r.getS3Key().isBlank()) {
            if (r.getDownloadUrl() != null && !r.getDownloadUrl().isBlank()) {
                return ResponseEntity.ok(Map.of("url", r.getDownloadUrl(), "expiresAt", (Object)null));
            }
            return ResponseEntity.status(409).body(Map.of("code","MISSING_S3KEY","message","Release sem s3Key nem downloadUrl"));
        }
        String url = cdnUploadService.presignGetUrl(r.getS3Key(), Duration.ofMinutes(15));
        String expiresAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plus(Duration.ofMinutes(15)));
        return ResponseEntity.ok(Map.of("url", url, "expiresAt", expiresAt));
    }

    private AppRelease.Platform parsePlatform(String p) {
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


