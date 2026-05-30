package com.projects.draftly.email.repository;

import com.projects.draftly.email.model.EmailThread;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailThreadRepository extends JpaRepository<EmailThread, Long> {
    Optional<EmailThread> findByGmailThreadId(String gmailThreadId);
}
