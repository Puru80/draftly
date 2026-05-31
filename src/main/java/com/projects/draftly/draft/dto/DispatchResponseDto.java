package com.projects.draftly.draft.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchResponseDto {
    private boolean success;
    private String message;
    private String gmailMessageId;
}