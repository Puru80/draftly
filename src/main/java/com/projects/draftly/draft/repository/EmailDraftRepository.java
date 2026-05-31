package com.projects.draftly.draft.repository;

import com.projects.draftly.draft.model.EmailDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailDraftRepository extends JpaRepository<EmailDraft, Long> {
    List<EmailDraft> findByThreadIdAndStatus(Long threadId, String status);

}