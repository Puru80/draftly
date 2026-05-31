package com.projects.draftly.email.controller;

import com.projects.draftly.auth.model.User;
import com.projects.draftly.auth.repository.UserRepository;
import com.projects.draftly.email.dto.EmailThreadResponseDto;
import com.projects.draftly.email.dto.ThreadDetailResponseDto;
import com.projects.draftly.email.dto.ThreadMessageDto;
import com.projects.draftly.email.model.EmailThread;
import com.projects.draftly.email.repository.EmailThreadRepository;
import com.projects.draftly.email.service.GmailApiService;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/threads")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class EmailController {

    private final EmailThreadRepository emailThreadRepository;
    private final UserRepository userRepository;
    private final GmailApiService gmailApiService;

    @GetMapping
    public ResponseEntity<List<EmailThreadResponseDto>> getThreads(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));

        List<EmailThread> threads = emailThreadRepository.findByUserOrderByLastSynchronizedAtDesc(user);

        List<EmailThreadResponseDto> dtos = threads.stream()
            .map(t -> EmailThreadResponseDto.builder()
                .id(t.getId())
                .gmailThreadId(t.getGmailThreadId())
                .subject(t.getSubject())
                .lastSynchronizedAt(t.getLastSynchronizedAt())
                .build())
            .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThreadDetailResponseDto> getThread(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal) throws Exception {
        String email = principal.getAttribute("email");
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));

        EmailThread thread = emailThreadRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Thread not found: " + id));

        if (!thread.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        com.google.api.services.gmail.model.Thread gmailThread =
            gmailApiService.fetchThread(user, thread.getGmailThreadId());

        List<ThreadMessageDto> messages = gmailThread.getMessages().stream()
            .sorted((a, b) -> {
                Long dateA = a.getInternalDate();
                Long dateB = b.getInternalDate();
                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.compareTo(dateA);
            })
            .map(this::toMessageDto)
            .toList();

        ThreadDetailResponseDto dto = ThreadDetailResponseDto.builder()
            .id(thread.getId())
            .gmailThreadId(thread.getGmailThreadId())
            .subject(thread.getSubject())
            .lastSynchronizedAt(thread.getLastSynchronizedAt())
            .messages(messages)
            .build();

        return ResponseEntity.ok(dto);
    }

    private ThreadMessageDto toMessageDto(Message message) {
        String body = null;
        if (message.getPayload() != null) {
            body = gmailApiService.decodeBody(message.getPayload());
        }

        return ThreadMessageDto.builder()
            .messageId(message.getId())
            .from(gmailApiService.extractHeader(message, "From"))
            .to(gmailApiService.extractHeader(message, "To"))
            .subject(gmailApiService.extractHeader(message, "Subject"))
            .date(gmailApiService.extractHeader(message, "Date"))
            .snippet(message.getSnippet())
            .body(body)
            .build();
    }
}
