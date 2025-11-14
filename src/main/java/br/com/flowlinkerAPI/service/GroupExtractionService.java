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
                java.util.List<GroupExtractionRequestDTO.SimpleGroupDTO> unique = new java.util.ArrayList<>();
                for (GroupExtractionRequestDTO.SimpleGroupDTO g : payload.getGroups()) {
                    if (g == null) continue;
                    String extId = g.getExternalId();
                    String url = g.getUrl();
                    boolean duplicate = false;
                    if (extId != null && !extId.isBlank() && !seenExternal.add(extId)) duplicate = true;
                    if (url != null && !url.isBlank() && !seenUrls.add(url)) duplicate = true;
                    if (!duplicate) unique.add(g);
                }

                java.util.Set<String> extIds = unique.stream()
                        .map(GroupExtractionRequestDTO.SimpleGroupDTO::getExternalId)
                        .filter(id -> id != null && !id.isBlank())
                        .collect(java.util.stream.Collectors.toSet());
                java.util.Set<String> urls = unique.stream()
                        .map(GroupExtractionRequestDTO.SimpleGroupDTO::getUrl)
                        .filter(u -> u != null && !u.isBlank())
                        .collect(java.util.stream.Collectors.toSet());

                java.util.Map<String, GroupCatalog> byExtId = new java.util.HashMap<>();
                java.util.Map<String, GroupCatalog> byUrl = new java.util.HashMap<>();
                if (!extIds.isEmpty()) {
                    for (GroupCatalog gc : groupCatalogRepository.findByExternalIdIn(extIds)) {
                        if (gc.getExternalId() != null) byExtId.put(gc.getExternalId(), gc);
                    }
                }
                if (!urls.isEmpty()) {
                    for (GroupCatalog gc : groupCatalogRepository.findByUrlIn(urls)) {
                        if (gc.getUrl() != null) byUrl.put(gc.getUrl(), gc);
                    }
                }

                java.util.List<GroupCatalog> toCreate = new java.util.ArrayList<>();
                java.util.List<GroupCatalog> toUpdate = new java.util.ArrayList<>();
                for (GroupExtractionRequestDTO.SimpleGroupDTO g : unique) {
                    String extId = g.getExternalId();
                    String url = g.getUrl();
                    GroupCatalog existing = (extId != null && byExtId.containsKey(extId)) ? byExtId.get(extId)
                            : (url != null ? byUrl.get(url) : null);
                    if (existing == null) {
                        GroupCatalog gc = new GroupCatalog();
                        gc.setExternalId(extId);
                        gc.setName(sanitizeGroupName(g.getName()));
                        gc.setUrl(url);
                        gc.setMemberCount(g.getMemberCount());
                        gc.setLastSeenAt(Instant.now());
                        toCreate.add(gc);
                    } else {
                        existing.setName(sanitizeGroupName(g.getName()));
                        existing.setUrl(url);
                        existing.setMemberCount(g.getMemberCount());
                        existing.setLastSeenAt(Instant.now());
                        toUpdate.add(existing);
                    }
                }

                if (!toCreate.isEmpty()) {
                    java.util.List<GroupCatalog> created = groupCatalogRepository.saveAll(toCreate);
                    for (GroupCatalog gc : created) {
                        if (gc.getExternalId() != null) byExtId.put(gc.getExternalId(), gc);
                        if (gc.getUrl() != null) byUrl.put(gc.getUrl(), gc);
                    }
                }
                if (!toUpdate.isEmpty()) {
                    groupCatalogRepository.saveAll(toUpdate);
                }

                java.util.List<GroupExtractionItem> items = new java.util.ArrayList<>(unique.size());
                for (GroupExtractionRequestDTO.SimpleGroupDTO g : unique) {
                    String extId = g.getExternalId();
                    String url = g.getUrl();
                    GroupCatalog gc = (extId != null && byExtId.containsKey(extId)) ? byExtId.get(extId)
                            : (url != null ? byUrl.get(url) : null);
                    if (gc == null) continue;
                    GroupExtractionItem item = new GroupExtractionItem();
                    item.setExtraction(extraction);
                    item.setGroup(gc);
                    items.add(item);
                }
                if (!items.isEmpty()) {
                    groupExtractionItemRepository.saveAll(items);
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


