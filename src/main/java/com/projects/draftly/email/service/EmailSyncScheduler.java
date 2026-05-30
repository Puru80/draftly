package com.projects.draftly.email.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.projects.draftly.auth.model.User;
import com.projects.draftly.auth.repository.UserRepository;
import com.projects.draftly.email.model.EmailThread;
import com.projects.draftly.email.repository.EmailThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSyncScheduler {

    private final UserRepository userRepository;
    private final EmailThreadRepository threadRepository;
    private final GmailApiService gmailApiService;

    // Fires every 5 minutes (300,000 ms). Remember to add @EnableScheduling to your main application class!
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void executeIngestionPipeline() {
        log.info("Executing global asynchronous background ingestion engine...");
        List<User> activeUsers = userRepository.findAll();

        for (User user : activeUsers) {
            try {
                processUserInbox(user);
            } catch (Exception e) {
                log.error("Failed executing ingestion synchronization loop for user: {}", user.getEmail(), e);
                // Failures here are trapped safely per user record so one broken token doesn't crash the loop
            }
        }
    }

    private void processUserInbox(User user) throws Exception {
        Gmail gmail = gmailApiService.getGmailClient(user);

        // Fetch only unread metadata markers
        ListMessagesResponse response = gmail.users().messages().list("me")
            .setQ("is:unread")
            .setMaxResults(10L)
            .execute();

        List<Message> messageStubs = response.getMessages();
        if (messageStubs == null || messageStubs.isEmpty()) return;

        for (Message stub : messageStubs) {
            // Pull the full structural detail of the payload
            Message fullMessage = gmail.users().messages().get("me", stub.getId()).execute();
            String threadId = fullMessage.getThreadId();

            // Check if we already hold an record for this thread context
            if (threadRepository.findByGmailThreadId(threadId).isEmpty()) {
                String subject = extractHeader(fullMessage, "Subject");

                EmailThread newThread = EmailThread.builder()
                    .user(user)
                    .gmailThreadId(threadId)
                    .subject(subject != null ? subject : "(No Subject)")
                    .lastSynchronizedAt(ZonedDateTime.now())
                    .build();

                threadRepository.save(newThread);
                log.info("Successfully ingested new conversational node: Thread ID {}", threadId);

                // TODO: Fire Asynchronous Spring Event here to pass this record immediately over to the AI Draft Engine!
            }
        }
    }

    private String extractHeader(Message message, String headerName) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) return null;
        return message.getPayload().getHeaders().stream()
            .filter(h -> h.getName().equalsIgnoreCase(headerName))
            .map(com.google.api.services.gmail.model.MessagePartHeader::getValue)
            .findFirst()
            .orElse(null);
    }
}