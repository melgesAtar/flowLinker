package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.dto.app.UpdateCheckResponse;
import br.com.flowlinkerAPI.service.AppUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/app/update")
public class AppUpdateController {

    private final AppUpdateService appUpdateService;

    public AppUpdateController(AppUpdateService appUpdateService) {
        this.appUpdateService = appUpdateService;
    }

    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam String platform,
                                   @RequestParam String arch,
                                   @RequestParam String currentVersion) {
        Optional<UpdateCheckResponse> resp = appUpdateService.check(platform, arch, currentVersion);
        return resp.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}


