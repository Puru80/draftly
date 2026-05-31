package com.projects.draftly.email.dto;

import lombok.*;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadDetailResponseDto {
    private Long id;
    private String gmailThreadId;
    private String subject;
    private ZonedDateTime lastSynchronizedAt;
    private List<ThreadMessageDto> messages;
}
