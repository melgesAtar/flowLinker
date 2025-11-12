package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.dto.desktop.GroupExtractionRequestDTO;
import br.com.flowlinkerAPI.model.*;
import br.com.flowlinkerAPI.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import br.com.flowlinkerAPI.config.security.CurrentRequest;
import br.com.flowlinkerAPI.dto.desktop.ExtractionSummaryDTO;
import br.com.flowlinkerAPI.dto.desktop.ExtractionGroupDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class GroupExtractionService {

    private final GroupCatalogRepository groupCatalogRepository;
    private final GroupExtractionRepository groupExtractionRepository;
    private final GroupExtractionItemRepository groupExtractionItemRepository;
    private final br.com.flowlinkerAPI.repository.CustomerRepository customerRepository;
    private final SocialMediaAccountRepository socialMediaAccountRepository;
    private final CurrentRequest currentRequest;
    private static final Logger logger = LoggerFactory.getLogger(GroupExtractionService.class);

    @Async("appTaskExecutor")
    @Transactional
    public void processExtraction(GroupExtractionRequestDTO payload) {
        if (payload == null) return;

        long started = System.currentTimeMillis();
        GroupExtraction extraction = null;
        Device device = currentRequest.getDevice();
        Customer customer = null;

        Long customerId = currentRequest.getCustomerId();
        try {
            int groupsSize = payload.getGroups() != null ? payload.getGroups().size() : 0;
            logger.info("[Extraction] START cid={} deviceFp={} account={} keywords={} groups={}",
                customerId,
                currentRequest.getDeviceFingerprint(),
                payload.getAccountUsername(),
                payload.getKeywords(),
                groupsSize);

            if (device != null) {
                customer = device.getCustomer();
            } else if (customerId != null) {
                customer = customerRepository.findById(customerId).orElse(null);
            }

            extraction = new GroupExtraction();
            extraction.setDevice(device);
            extraction.setCustomer(customer);
            extraction.setExtractedAt(payload.getExtractedAt() != null ? payload.getExtractedAt() : Instant.now());
            extraction.setKeywordsText(joinKeywords(payload.getKeywords()));
            extraction.setGroupsCount(groupsSize);

            if (customer != null && payload.getAccountUsername() != null && !payload.getAccountUsername().isBlank()) {
                socialMediaAccountRepository.findFirstByCustomerIdAndUsernameIgnoreCase(customer.getId(), payload.getAccountUsername())
                    .ifPresent(extraction::setSocialAccount);
            }
            extraction = groupExtractionRepository.save(extraction);

            if (payload.getGroups() != null) {
                java.util.Set<String> seenExternal = new java.util.HashSet<>();
                java.util.Set<String> seenUrls = new java.util.HashSet<>();
                for (GroupExtractionRequestDTO.SimpleGroupDTO g : payload.getGroups()) {
                    if (g == null) continue;

                    String extId = g.getExternalId();
                    String url = g.getUrl();
                    // Dedup dentro do mesmo payload
                    if (extId != null && !extId.isBlank() && !seenExternal.add(extId)) continue;
                    if (url != null && !url.isBlank() && !seenUrls.add(url)) continue;

                    GroupCatalog grp = groupCatalogRepository.findByExternalId(extId)
                            .orElseGet(() -> groupCatalogRepository.findByUrl(url).orElse(null));
                    if (grp == null) {
                        grp = new GroupCatalog();
                        grp.setExternalId(extId);
                        grp.setName(sanitizeGroupName(g.getName()));
                        grp.setUrl(url);
                        grp.setMemberCount(g.getMemberCount());
                        grp.setLastSeenAt(Instant.now());
                    } else {
                        grp.setName(sanitizeGroupName(g.getName()));
                        grp.setUrl(url);
                        grp.setMemberCount(g.getMemberCount());
                        grp.setLastSeenAt(Instant.now());
                    }
                    try {
                        grp = groupCatalogRepository.save(grp);
                    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                        // Corrida/duplicata: recarrega existente e atualiza
                        GroupCatalog existing = null;
                        if (extId != null && !extId.isBlank()) {
                            existing = groupCatalogRepository.findByExternalId(extId).orElse(null);
                        }
                        if (existing == null && url != null && !url.isBlank()) {
                            existing = groupCatalogRepository.findByUrl(url).orElse(null);
                        }
                        if (existing != null) {
                            existing.setName(grp.getName());
                            existing.setUrl(grp.getUrl());
                            existing.setMemberCount(grp.getMemberCount());
                            existing.setLastSeenAt(grp.getLastSeenAt());
                            grp = groupCatalogRepository.save(existing);
                        } else {
                            throw ex;
                        }
                    }

                    GroupExtractionItem item = new GroupExtractionItem();
                    item.setExtraction(extraction);
                    item.setGroup(grp);
                    groupExtractionItemRepository.save(item);
                }
            }

            logger.info("[Extraction] END cid={} extractionId={} durationMs={}",
                customerId,
                extraction != null ? extraction.getId() : null,
                System.currentTimeMillis() - started);
        } catch (Exception e) {
            logger.error("[Extraction] ERROR cid={} msg={}", customerId, e.getMessage(), e);
            throw e;
        }
    }

    private String joinKeywords(List<String> kws) {
        if (kws == null || kws.isEmpty()) return null;
        return kws.stream().filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(", "));
    }

    private String sanitizeGroupName(String raw) {
        if (raw == null) return null;
        String name = raw.trim();
        // Remove prefixos automáticos do Facebook como "Foto do perfil de|da|do|das|dos " ou
        // variações "Foto de perfil de|da|do|das|dos ", case-insensitive
        name = name.replaceFirst("(?i)^foto\\s+do\\s+perfil\\s+d(?:e|a|o|as|os)\\s+", "");
        name = name.replaceFirst("(?i)^foto\\s+de\\s+perfil\\s+d(?:e|a|o|as|os)\\s+", "");
        return name.trim();
    }

    

    @Transactional
    public void deleteExtraction(Long extractionId) {
        if (extractionId == null) return;
        Long customerId = currentRequest.getCustomerId();
        GroupExtraction extraction = groupExtractionRepository.findById(extractionId).orElse(null);
        if (extraction == null) return;
        if (extraction.getCustomer() != null && customerId != null && !extraction.getCustomer().getId().equals(customerId)) {
            return;
        }
        // Como a entidade possui orphanRemoval, os itens serão apagados junto
        groupExtractionRepository.delete(extraction);
    }

    @Transactional(readOnly = true)
    public Page<ExtractionSummaryDTO> listSummaries(int page, int size) {
        Long customerId = currentRequest.getCustomerId();
        if (customerId == null) return Page.empty();
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) return Page.empty();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "extractedAt"));
        Page<GroupExtraction> p = groupExtractionRepository.findByCustomerOrderByExtractedAtDesc(customer, pageable);
        return p.map(this::toSummaryDTO);
    }

    @Transactional(readOnly = true)
    public Page<ExtractionGroupDTO> listGroups(Long extractionId, int page, int size) {
        if (extractionId == null) return Page.empty();
        Long customerId = currentRequest.getCustomerId();
        GroupExtraction extraction = groupExtractionRepository.findById(extractionId).orElse(null);
        if (extraction == null) return Page.empty();
        if (extraction.getCustomer() != null && customerId != null && !extraction.getCustomer().getId().equals(customerId)) {
            return Page.empty();
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<GroupExtractionItem> items = groupExtractionItemRepository.findByExtraction(extraction, pageable);
        return items.map(it -> toGroupDTO(it.getGroup()));
    }

    private ExtractionSummaryDTO toSummaryDTO(GroupExtraction e) {
        ExtractionSummaryDTO dto = new ExtractionSummaryDTO();
        dto.setId(e.getId());
        dto.setExtractedAt(e.getExtractedAt());
        dto.setGroupsCount(e.getGroupsCount());
        dto.setKeywords(e.getKeywordsText());
        if (e.getSocialAccount() != null) {
            dto.setSocialAccountUsername(e.getSocialAccount().getUsername());
            dto.setSocialAccountPlatform(e.getSocialAccount().getPlatform() != null ? e.getSocialAccount().getPlatform().name() : null);
        }
        if (e.getDevice() != null) {
            dto.setDeviceId(e.getDevice().getDeviceId());
            dto.setDeviceFingerprint(e.getDevice().getFingerprint());
        }
        return dto;
    }

    private ExtractionGroupDTO toGroupDTO(GroupCatalog g) {
        ExtractionGroupDTO dto = new ExtractionGroupDTO();
        dto.setGroupId(g.getId());
        dto.setExternalId(g.getExternalId());
        dto.setName(g.getName());
        dto.setUrl(g.getUrl());
        dto.setMemberCount(g.getMemberCount());
        dto.setLastSeenAt(g.getLastSeenAt());
        return dto;
    }

}


