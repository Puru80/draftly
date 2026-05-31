package com.projects.draftly.draft.dto;

import lombok.Data;

@Data
public class ToneUpdateRequest {
    private String requestedTone; // FORMAL, CONCISE, FRIENDLY
    private String incomingEmailBody; // Re-supplied to preserve generation context
}
