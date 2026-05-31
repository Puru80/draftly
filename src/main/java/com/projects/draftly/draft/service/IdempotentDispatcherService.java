package com.projects.draftly.draft.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.projects.draftly.draft.dto.DispatchResponseDto;
import com.projects.draftly.draft.model.EmailDraft;
import com.projects.draftly.draft.repository.EmailDraftRepository;
import com.projects.draftly.email.service.GmailApiService;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.ZonedDateTime;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentDispatcherService {

    private final EmailDraftRepository draftRepository;
    private final GmailApiService gmailApiService;

    @Transactional
    public DispatchResponseDto dispatchApprovedEmail(Long draftId, String idempotencyKey) {
        // 1. Fetch Draft and verify it hasn't already been processed
        EmailDraft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new EntityNotFoundException("Draft record not found with ID: " + draftId));

        if ("APPROVED".equalsIgnoreCase(draft.getStatus())) {
            return DispatchResponseDto.builder()
                .success(false)
                .message("Execution aborted: This specific draft has already been successfully dispatched.")
                .build();
        }

        // 2. Enforce Idempotency Key validation
        if (draft.getIdempotencyKey() != null && draft.getIdempotencyKey().equals(idempotencyKey)) {
            return DispatchResponseDto.builder()
                .success(false)
                .message("Duplicate execution caught via Idempotency Token identification.")
                .build();
        }

        try {
            // 3. Initialize Authenticated Gmail client
            Gmail gmailClient = gmailApiService.getGmailClient(draft.getThread().getUser());

            // 4. Retrieve the original parent message from Google to extract thread mapping headers
            Message parentMessage = gmailClient.users().messages()
                .get("me", draft.getThread().getGmailThreadId()).execute();

            String originalMessageId = extractHeader(parentMessage, "Message-ID");
            String originalReferences = extractHeader(parentMessage, "References");
            String originalSender = extractHeader(parentMessage, "From");

            // 5. Construct Raw MIME Message Architecture
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage mimeMessage = new MimeMessage(session);

            mimeMessage.setFrom(new InternetAddress("me"));
            mimeMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(originalSender));
            mimeMessage.setSubject("Re: " + draft.getThread().getSubject());
            mimeMessage.setText(draft.getSuggestedBody());

            // Threading Continuity Rules:
            // - In-Reply-To must point to the immediate parent's Message-ID.
            // - References should append the new ID to the existing historical list.
            if (originalMessageId != null) {
                mimeMessage.setHeader("In-Reply-To", originalMessageId);
                String updatedReferences = (originalReferences != null) ? originalReferences + " " + originalMessageId : originalMessageId;
                mimeMessage.setHeader("References", updatedReferences);
            }

            // 6. Encode the MIME email payload to Base64url format required by Gmail API
            ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(bytesStream);
            String encodedEmail = Base64.encodeBase64URLSafeString(bytesStream.toByteArray());

            Message outboundMessage = new Message();
            outboundMessage.setRaw(encodedEmail);
            outboundMessage.setThreadId(draft.getThread().getGmailThreadId()); // Forces thread nesting in client views

            // 7. Execute Dispatch Delivery
            Message executedResponse = gmailClient.users().messages().send("me", outboundMessage).execute();

            // 8. Commit Transaction States locally
            draft.setStatus("APPROVED");
            draft.setIdempotencyKey(idempotencyKey);
            draft.setUpdatedAt(ZonedDateTime.now());
            draftRepository.save(draft);

            log.info("Successfully sent email thread id: {}. Gmail Message ID: {}", draft.getThread().getGmailThreadId(), executedResponse.getId());

            return DispatchResponseDto.builder()
                .success(true)
                .message("Email successfully sent.")
                .gmailMessageId(executedResponse.getId())
                .build();

        } catch (Exception e) {
            log.error("Critical failure during outbound dispatch pipeline execution for draft ID: {}", draftId, e);
            return DispatchResponseDto.builder()
                .success(false)
                .message("Failed executing mail delivery pipeline: " + e.getMessage())
                .build();
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
