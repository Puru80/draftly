package com.projects.draftly.email.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadMessageDto {
    private String messageId;
    private String from;
    private String to;
    private String subject;
    private String date;
    private String snippet;
    private String body;
}
