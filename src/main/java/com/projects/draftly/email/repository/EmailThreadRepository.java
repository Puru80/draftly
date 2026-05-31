package com.projects.draftly.email.repository;

import com.projects.draftly.auth.model.User;
import com.projects.draftly.email.model.EmailThread;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmailThreadRepository extends JpaRepository<EmailThread, Long> {
    Optional<EmailThread> findByGmailThreadId(String gmailThreadId);
    List<EmailThread> findByUserOrderByLastSynchronizedAtDesc(User user);
}
