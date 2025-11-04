package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.dto.desktop.GroupExtractionRequestDTO;
import br.com.flowlinkerAPI.dto.desktop.ExtractionSummaryDTO;
import br.com.flowlinkerAPI.dto.desktop.ExtractionGroupDTO;
import br.com.flowlinkerAPI.service.GroupExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/extractions")
@RequiredArgsConstructor
public class ExtractionController {

    private final GroupExtractionService groupExtractionService;

    @PostMapping
    public ResponseEntity<Void> createExtraction(@RequestBody GroupExtractionRequestDTO payload) {
        groupExtractionService.processExtraction(payload);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{extractionId}/groups")
    public ResponseEntity<Void> addGroups(@PathVariable Long extractionId,
                                          @RequestBody List<GroupExtractionRequestDTO.SimpleGroupDTO> groups) {
        groupExtractionService.addGroupsToExtraction(extractionId, groups);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{extractionId}/groups/{groupId}")
    public ResponseEntity<Void> removeGroup(@PathVariable Long extractionId,
                                            @PathVariable Long groupId) {
        groupExtractionService.removeGroupFromExtraction(extractionId, groupId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{extractionId}")
    public ResponseEntity<Void> deleteExtraction(@PathVariable Long extractionId) {
        groupExtractionService.deleteExtraction(extractionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summaries")
    public ResponseEntity<Page<ExtractionSummaryDTO>> listSummaries(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(groupExtractionService.listSummaries(page, size));
    }

    @GetMapping("/{extractionId}/groups")
    public ResponseEntity<Page<ExtractionGroupDTO>> listGroups(@PathVariable Long extractionId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(groupExtractionService.listGroups(extractionId, page, size));
    }
}


