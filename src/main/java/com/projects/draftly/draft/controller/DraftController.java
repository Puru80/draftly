package com.projects.draftly.draft.controller;

import com.projects.draftly.draft.dto.BodyUpdateRequest;
import com.projects.draftly.draft.dto.DispatchResponseDto;
import com.projects.draftly.draft.dto.DraftResponseDto;
import com.projects.draftly.draft.dto.ToneUpdateRequest;
import com.projects.draftly.draft.service.DraftManagementService;
import com.projects.draftly.draft.service.IdempotentDispatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drafts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // Permissive mapping allowing local React development execution
public class DraftController {

    private final DraftManagementService draftManagementService;
    private final IdempotentDispatcherService idempotentDispatcherService;

    @GetMapping("/pending")
    public ResponseEntity<List<DraftResponseDto>> fetchAllPendingDrafts() {
        return ResponseEntity.ok(draftManagementService.getPendingDrafts());
    }

    @PutMapping("/{id}/body")
    public ResponseEntity<DraftResponseDto> editDraftContent(
        @PathVariable Long id,
        @RequestBody BodyUpdateRequest request) {
        return ResponseEntity.ok(draftManagementService.updateDraftBody(id, request.getUpdatedBody()));
    }

    @PostMapping("/{id}/tone")
    public ResponseEntity<DraftResponseDto> switchDraftTone(
        @PathVariable Long id,
        @RequestBody ToneUpdateRequest request) {
        return ResponseEntity.ok(draftManagementService.regenerateDraftWithNewTone(
            id,
            request.getRequestedTone(),
            request.getIncomingEmailBody()
        ));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<DispatchResponseDto> finalizeAndSendDraft(
        @PathVariable Long id,
        @RequestHeader("X-Idempotency-Key") String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(DispatchResponseDto.builder()
                .success(false)
                .message("Missing required mandatory HTTP Header constraint: X-Idempotency-Key")
                .build());
        }

        DispatchResponseDto result = idempotentDispatcherService.dispatchApprovedEmail(id, idempotencyKey);
        if (!result.isSuccess()) {
            return ResponseEntity.status(409).body(result); // Return 409 Conflict if idempotency lock flags hit
        }

        return ResponseEntity.ok(result);
    }
}
