package com.projects.draftly.draft.model;

import com.projects.draftly.email.model.EmailThread;
import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "drafts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private EmailThread thread;

    @Column(name = "suggested_body", nullable = false, columnDefinition = "TEXT")
    private String suggestedBody;

    @Column(name = "current_tone", nullable = false, length = 50)
    private String currentTone; // FORMAL, CONCISE, FRIENDLY

    @Column(nullable = false, length = 50)
    private String status; // PENDING, APPROVED, EDITED, REJECTED

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now();
}