package com.projects.draftly.ai.service;


import com.projects.draftly.auth.model.User;
import com.projects.draftly.draft.model.EmailDraft;
import com.projects.draftly.draft.repository.EmailDraftRepository;
import com.projects.draftly.email.model.EmailThread;
import com.projects.draftly.email.service.GmailApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDraftEngineService {

    private final ChatClient.Builder chatClientBuilder;
    private final GmailApiService gmailApiService;
    private final EmailDraftRepository draftRepository;

    public EmailDraft generateDraftForThread(EmailThread thread, String requestedTone, String incomingEmailBody) {
        User user = thread.getUser();
        String writingStyleExamples = "No historical examples found.";

        try {
            // 1. Fetch Few-Shot Writing Patterns
            List<String> history = gmailApiService.fetchUserSentHistory(user, 3);
            if (!history.isEmpty()) {
                writingStyleExamples = String.join("\n---\nSample: ", history);
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user writing patterns for style mimicry, defaulting to neutral generation.", e);
        }

        // 2. Build the System Blueprint Prompt
        String systemPrompt = """
            You are an advanced executive email assistant. Your core directive is to draft an exceptional reply to an incoming email on behalf of the user.
            
            CRITICAL DIRECTIVES:
            1. Analyze the USER'S WRITING SAMPLES below. Adopt their structure, greeting preferences, level of vocabulary, and formatting habits. Do not copy facts from the samples; only replicate the style.
            2. Match the REQUESTED TONE constraints accurately.
            3. Append the user's custom signature if provided.
            4. Output ONLY the response body. Do not include greeting placeholders like '[Infert Subject Here]' or conversational filler.
            
            USER'S WRITING SAMPLES (STYLE GUIDE):
            [SAMPLES_START]
            %s
            [SAMPLES_END]
            
            REQUESTED TONE CONSTRAINT: %s
            USER SIGNATURE: %s
            """.formatted(writingStyleExamples, getToneInstruction(requestedTone),
            user.getCustomSignature() != null ? user.getCustomSignature() : "None");

        // 3. Fire the LLM request using Spring AI Fluent API
        ChatClient chatClient = chatClientBuilder.build();
        String generatedReply = chatClient.prompt()
            .system(systemPrompt)
            .user("Incoming Email Content to reply to:\n" + incomingEmailBody)
            .call()
            .content();

        // 4. Save the generated asset to the persistent draft matrix
        EmailDraft draft = EmailDraft.builder()
            .thread(thread)
            .suggestedBody(generatedReply)
            .currentTone(requestedTone.toUpperCase())
            .status("PENDING")
            .updatedAt(ZonedDateTime.now())
            .build();

        return draftRepository.save(draft);
    }

    private String getToneInstruction(String tone) {
        return switch (tone.toUpperCase()) {
            case "CONCISE" -> "Keep the response under 2-3 sentences. Short, direct, and action-oriented.";
            case "FORMAL" -> "Use high-caliber corporate vernacular. Structured, respectful, and thoroughly professional.";
            case "FRIENDLY" -> "Warm, welcoming, conversational, and light-hearted while preserving professional boundaries.";
            default -> "Professional, concise, and neutral.";
        };
    }
}
