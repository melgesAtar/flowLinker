package br.com.flowlinkerAPI.dto.campaign;

import java.time.Instant;
import java.util.List;

public class CampaignResumeResponse {
    public Long campaignId;
    public String status;
    public Instant startedAt;
    public Long extractionId;
    public Integer totalGroups;
    public Integer lastProcessedIndex;

    public String message;
    public String linkUrl;

    public Integer rotateAccountEveryNShares;
    public Integer typingDelayMs;
    public Integer postIntervalDelayMs;
    public Integer clickButtonsDelayMs;

    public List<Long> accountIds;
    public List<Long> conflictingAccountIds; // contas também ativas em outras campanhas RUNNING
}


