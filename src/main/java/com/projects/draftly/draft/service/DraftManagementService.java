package com.projects.draftly.draft.service;

import com.projects.draftly.ai.service.AiDraftEngineService;
import com.projects.draftly.draft.dto.DraftResponseDto;
import com.projects.draftly.draft.model.EmailDraft;
import com.projects.draftly.draft.repository.EmailDraftRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DraftManagementService {

    private final EmailDraftRepository draftRepository;
    private final AiDraftEngineService aiDraftEngineService;

    @Transactional(readOnly = true)
    public List<DraftResponseDto> getPendingDrafts() {
        // Fetch rows still waiting for human intervention
        return draftRepository.findAll().stream()
            .filter(draft -> "PENDING".equalsIgnoreCase(draft.getStatus()))
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public DraftResponseDto updateDraftBody(Long draftId, String newBody) {
        EmailDraft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new EntityNotFoundException("Draft record not found with ID: " + draftId));

        draft.setSuggestedBody(newBody);
        draft.setStatus("EDITED"); // Shift state tracking if the human manually typed corrections
        draft.setUpdatedAt(ZonedDateTime.now());

        return mapToDto(draftRepository.save(draft));
    }

    @Transactional
    public DraftResponseDto regenerateDraftWithNewTone(Long draftId, String targetTone, String emailBody) {
        EmailDraft oldDraft = draftRepository.findById(draftId)
            .orElseThrow(() -> new EntityNotFoundException("Draft record not found with ID: " + draftId));

        // Mark old alternative output as rejected/stale
        oldDraft.setStatus("REJECTED");
        draftRepository.save(oldDraft);

        // Call our AI Core to issue a completely fresh generation sequence
        EmailDraft freshDraft = aiDraftEngineService.generateDraftForThread(
            oldDraft.getThread(),
            targetTone,
            emailBody
        );

        return mapToDto(freshDraft);
    }

    private DraftResponseDto mapToDto(EmailDraft draft) {
        return DraftResponseDto.builder()
            .id(draft.getId())
            .threadId(draft.getThread().getId())
            .gmailThreadId(draft.getThread().getGmailThreadId())
            .subject(draft.getThread().getSubject())
            .suggestedBody(draft.getSuggestedBody())
            .currentTone(draft.getCurrentTone())
            .status(draft.getStatus())
            .updatedAt(draft.getUpdatedAt())
            .build();
    }
}
