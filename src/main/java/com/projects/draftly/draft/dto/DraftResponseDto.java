package com.projects.draftly.draft.dto;

import lombok.*;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftResponseDto {
    private Long id;
    private Long threadId;
    private String gmailThreadId;
    private String subject;
    private String suggestedBody;
    private String currentTone;
    private String status;
    private ZonedDateTime updatedAt;
}
