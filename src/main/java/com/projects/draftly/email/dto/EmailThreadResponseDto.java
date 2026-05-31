package com.projects.draftly.email.dto;

import lombok.*;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailThreadResponseDto {
    private Long id;
    private String gmailThreadId;
    private String subject;
    private ZonedDateTime lastSynchronizedAt;
}
