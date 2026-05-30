package com.projects.draftly.email.model;

import com.projects.draftly.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "email_threads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "gmail_thread_id", unique = true, nullable = false)
    private String gmailThreadId;

    @Column(length = 500)
    private String subject;

    @Column(name = "last_synchronized_at")
    private ZonedDateTime lastSynchronizedAt = ZonedDateTime.now();
}