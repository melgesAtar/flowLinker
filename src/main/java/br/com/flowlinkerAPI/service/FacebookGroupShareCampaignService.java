package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.dto.campaign.CampaignProgressResponse;
import br.com.flowlinkerAPI.dto.campaign.CampaignResumeResponse;
import br.com.flowlinkerAPI.dto.campaign.CampaignProgressUpdateRequest;
import br.com.flowlinkerAPI.dto.campaign.FacebookGroupShareStartRequest;
import br.com.flowlinkerAPI.dto.campaign.FacebookGroupShareStartResponse;
import br.com.flowlinkerAPI.model.*;
import br.com.flowlinkerAPI.repository.*;
import br.com.flowlinkerAPI.config.security.CurrentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class FacebookGroupShareCampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignTypeRepository campaignTypeRepository;
    private final FacebookGroupShareCampaignRepository fbCampaignRepository;
    private final CampaignAccountRepository campaignAccountRepository;
    private final GroupExtractionRepository groupExtractionRepository;
    private final SocialMediaAccountRepository socialAccountRepository;
    private final CurrentRequest currentRequest;

    public FacebookGroupShareCampaignService(CampaignRepository campaignRepository,
                                             CampaignTypeRepository campaignTypeRepository,
                                             FacebookGroupShareCampaignRepository fbCampaignRepository,
                                             CampaignAccountRepository campaignAccountRepository,
                                             GroupExtractionRepository groupExtractionRepository,
                                             SocialMediaAccountRepository socialAccountRepository,
                                             CurrentRequest currentRequest) {
        this.campaignRepository = campaignRepository;
        this.campaignTypeRepository = campaignTypeRepository;
        this.fbCampaignRepository = fbCampaignRepository;
        this.campaignAccountRepository = campaignAccountRepository;
        this.groupExtractionRepository = groupExtractionRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.currentRequest = currentRequest;
    }

    @Transactional
    public FacebookGroupShareStartResponse start(FacebookGroupShareStartRequest req) {
        if (req == null || req.extractionId == null || req.accountIds == null || req.accountIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados inválidos");
        }

        Long customerId = currentRequest.getCustomerId();
        Device device = currentRequest.getDevice();
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cliente não identificado");
        }

        // Regra: 1 campanha por device e por TIPO (RUNNING/PAUSED). Aqui: FB_GROUP_SHARE
        if (device != null && device.getId() != null) {
            var existingForDevice = campaignRepository.findByDeviceAndTypeCodeAndStatuses(
                device.getId(),
                "FB_GROUP_SHARE",
                java.util.List.of(CampaignStatus.RUNNING, CampaignStatus.PAUSED)
            );
            if (existingForDevice != null && !existingForDevice.isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O dispositivo já possui campanha FB_GROUP_SHARE em andamento/pausada. Conclua ou cancele antes de iniciar outra."
                );
            }
        }

        GroupExtraction extraction = groupExtractionRepository.findById(req.extractionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Extração não encontrada"));
        if (extraction.getCustomer() == null || !extraction.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Extração não pertence ao cliente");
        }

        // Valida contas em uso em campanhas RUNNING/PAUSED
        var conflicts = campaignAccountRepository.findByAccountIdsAndCampaignStatuses(
            req.accountIds,
            java.util.List.of(CampaignStatus.RUNNING, CampaignStatus.PAUSED)
        );
        if (conflicts != null && !conflicts.isEmpty()) {
            // filtra apenas conflitos de outras campanhas, caso esteja retomando
            String used = conflicts.stream()
                .map(ca -> ca.getSocialAccount() != null ? ca.getSocialAccount().getUsername() : String.valueOf(ca.getId()))
                .distinct()
                .reduce((a,b) -> a + ", " + b).orElse("");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contas em uso em campanhas ativas: " + used);
        }

        CampaignType type = campaignTypeRepository.findByCode("FB_GROUP_SHARE")
            .orElseGet(() -> {
                CampaignType t = new CampaignType();
                t.setCode("FB_GROUP_SHARE");
                t.setName("Compartilhamento em grupos facebook");
                return campaignTypeRepository.save(t);
            });

        Campaign campaign = new Campaign();
        campaign.setType(type);
        campaign.setCustomer(extraction.getCustomer());
        campaign.setStartedByDevice(device);
        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setStartedAt(Instant.now());
        campaign = campaignRepository.save(campaign);

        FacebookGroupShareCampaign fb = new FacebookGroupShareCampaign();
        fb.setCampaign(campaign);
        fb.setExtractionUsed(extraction);
        fb.setMessage(req.message);
        fb.setLinkUrl(req.linkUrl);
        fb.setRotateAccountEveryNShares(req.rotateAccountEveryNShares);
        fb.setTypingDelayMs(req.typingDelayMs);
        fb.setPostIntervalDelayMs(req.postIntervalDelayMs);
        fb.setClickButtonsDelayMs(req.clickButtonsDelayMs);
        fb.setLastProcessedIndex(0);
        fbCampaignRepository.save(fb);

        // Vincula contas
        List<SocialMediaAccount> accounts = socialAccountRepository.findAllById(req.accountIds);
        for (SocialMediaAccount acc : accounts) {
            if (acc == null || acc.getCustomer() == null || !acc.getCustomer().getId().equals(customerId)) continue;
            CampaignAccount ca = new CampaignAccount();
            ca.setCampaign(campaign);
            ca.setSocialAccount(acc);
            campaignAccountRepository.save(ca);
        }

        FacebookGroupShareStartResponse resp = new FacebookGroupShareStartResponse();
        resp.campaignId = campaign.getId();
        resp.totalGroups = extraction.getGroupsCount() != null ? extraction.getGroupsCount() : 0;
        resp.lastProcessedIndex = 0;
        resp.status = campaign.getStatus().name();
        return resp;
    }

    @Transactional
    public void pause(Long campaignId, CampaignProgressUpdateRequest progress) {
        Campaign campaign = mustOwnCampaign(campaignId);
        campaign.setStatus(CampaignStatus.PAUSED);
        campaignRepository.save(campaign);
        updateProgressIfProvided(campaignId, progress);
    }

    @Transactional
    public void complete(Long campaignId, CampaignProgressUpdateRequest progress) {
        Campaign campaign = mustOwnCampaign(campaignId);
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaignRepository.save(campaign);
        updateProgressIfProvided(campaignId, progress);
    }

    @Transactional
    public void cancel(Long campaignId, CampaignProgressUpdateRequest progress) {
        Campaign campaign = mustOwnCampaign(campaignId);
        campaign.setStatus(CampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
        updateProgressIfProvided(campaignId, progress);
    }

    @Transactional(readOnly = true)
    public CampaignProgressResponse progress(Long campaignId) {
        Campaign campaign = mustOwnCampaign(campaignId);
        FacebookGroupShareCampaign fb = fbCampaignRepository.findById(campaign.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha FB não encontrada"));
        CampaignProgressResponse resp = new CampaignProgressResponse();
        resp.campaignId = campaign.getId();
        resp.lastProcessedIndex = fb.getLastProcessedIndex() != null ? fb.getLastProcessedIndex() : 0;
        resp.totalGroups = fb.getExtractionUsed() != null && fb.getExtractionUsed().getGroupsCount() != null ? fb.getExtractionUsed().getGroupsCount() : 0;
        resp.status = campaign.getStatus().name();
        return resp;
    }

    @Transactional(readOnly = true)
    public java.util.List<CampaignResumeResponse> listRunningForCurrentDevice() {
        var device = currentRequest.getDevice();
        if (device == null || device.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device não identificado");
        }
        var campaigns = campaignRepository.findAllByStartedByDeviceIdAndStatusIn(
            device.getId(),
            java.util.List.of(CampaignStatus.RUNNING, CampaignStatus.PAUSED)
        );
        var result = new java.util.ArrayList<CampaignResumeResponse>();
        for (Campaign c : campaigns) {
            var fbOpt = fbCampaignRepository.findById(c.getId());
            if (fbOpt.isEmpty()) continue; // ignora outras campanhas
            var fb = fbOpt.get();
            var dto = new CampaignResumeResponse();
            dto.campaignId = c.getId();
            dto.status = c.getStatus().name();
            dto.startedAt = c.getStartedAt();
            dto.extractionId = fb.getExtractionUsed() != null ? fb.getExtractionUsed().getId() : null;
            dto.totalGroups = fb.getExtractionUsed() != null && fb.getExtractionUsed().getGroupsCount() != null ? fb.getExtractionUsed().getGroupsCount() : 0;
            dto.lastProcessedIndex = fb.getLastProcessedIndex() != null ? fb.getLastProcessedIndex() : 0;
            dto.message = fb.getMessage();
            dto.linkUrl = fb.getLinkUrl();
            dto.rotateAccountEveryNShares = fb.getRotateAccountEveryNShares();
            dto.typingDelayMs = fb.getTypingDelayMs();
            dto.postIntervalDelayMs = fb.getPostIntervalDelayMs();
            dto.clickButtonsDelayMs = fb.getClickButtonsDelayMs();

            var cas = campaignAccountRepository.findAllByCampaignId(c.getId());
            var accountIds = cas.stream().map(ca -> ca.getSocialAccount() != null ? ca.getSocialAccount().getId() : null)
                .filter(java.util.Objects::nonNull).toList();
            dto.accountIds = accountIds;

            // checa conflitos das mesmas contas em outras campanhas RUNNING/PAUSED
            var conflicts = campaignAccountRepository.findByAccountIdsAndCampaignStatuses(
                accountIds,
                java.util.List.of(CampaignStatus.RUNNING, CampaignStatus.PAUSED)
            );
            var conflicting = conflicts.stream()
                .filter(ca -> ca.getCampaign() != null && !ca.getCampaign().getId().equals(c.getId()))
                .map(ca -> ca.getSocialAccount() != null ? ca.getSocialAccount().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
            dto.conflictingAccountIds = conflicting;

            result.add(dto);
        }
        return result;
    }

    private Campaign mustOwnCampaign(Long campaignId) {
        if (campaignId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID inválido");
        Long customerId = currentRequest.getCustomerId();
        Campaign c = campaignRepository.findById(campaignId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (c.getCustomer() == null || customerId == null || !c.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return c;
    }

    private void updateProgressIfProvided(Long campaignId, CampaignProgressUpdateRequest progress) {
        if (progress == null || progress.lastProcessedIndex == null) return;
        FacebookGroupShareCampaign fb = fbCampaignRepository.findById(campaignId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        fb.setLastProcessedIndex(Math.max(0, progress.lastProcessedIndex));
        fbCampaignRepository.save(fb);
    }
}


