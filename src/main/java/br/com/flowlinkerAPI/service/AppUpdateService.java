package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.dto.app.UpdateCheckResponse;
import br.com.flowlinkerAPI.model.AppRelease;
import br.com.flowlinkerAPI.model.AppRelease.Arch;
import br.com.flowlinkerAPI.model.AppRelease.Platform;
import br.com.flowlinkerAPI.repository.AppReleaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AppUpdateService {

    private final AppReleaseRepository appReleaseRepository;

    public AppUpdateService(AppReleaseRepository appReleaseRepository) {
        this.appReleaseRepository = appReleaseRepository;
    }

    public Optional<UpdateCheckResponse> check(String platform, String arch, String currentVersion) {
        Platform p = parsePlatform(platform);
        Arch a = parseArch(arch);
        var releaseOpt = appReleaseRepository.findFirstByPlatformAndArchAndIsActiveOrderByCreatedAtDesc(p, a, true);
        if (releaseOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No release for platform/arch");
        }
        AppRelease release = releaseOpt.get();
        if (!isNewer(release.getVersion(), currentVersion)) {
            return Optional.empty();
        }
        UpdateCheckResponse resp = new UpdateCheckResponse();
        resp.latestVersion = release.getVersion();
        resp.minimumVersion = release.getMinimumVersion();
        resp.forceUpdate = Boolean.TRUE.equals(release.getForceUpdate()) || (release.getMinimumVersion() != null && isNewer(release.getMinimumVersion(), currentVersion));
        resp.downloadUrl = release.getDownloadUrl();
        resp.fileSize = release.getFileSize();
        resp.sha256 = release.getSha256();
        resp.releaseNotes = release.getReleaseNotes();
        return Optional.of(resp);
    }

    private boolean isNewer(String v1, String v2) {
        if (v1 == null || v2 == null) return false;
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int ai = i < a.length ? parseIntSafe(a[i]) : 0;
            int bi = i < b.length ? parseIntSafe(b[i]) : 0;
            if (ai > bi) return true;
            if (ai < bi) return false;
        }
        return false;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9].*$", "")); } catch (Exception e) { return 0; }
    }

    private Platform parsePlatform(String p) {
        if (p == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "platform required");
        return switch (p.toLowerCase()) {
            case "win", "windows" -> Platform.WIN;
            case "mac", "darwin", "osx", "macos" -> Platform.MAC;
            case "linux" -> Platform.LINUX;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported platform: " + p);
        };
    }

    private Arch parseArch(String a) {
        if (a == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "arch required");
        return switch (a.toLowerCase()) {
            case "x64", "amd64" -> Arch.X64;
            case "arm64", "aarch64" -> Arch.ARM64;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported arch: " + a);
        };
    }
}


