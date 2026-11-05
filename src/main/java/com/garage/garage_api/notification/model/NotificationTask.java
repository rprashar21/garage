package com.garage.garage_api.notification.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "enquiry_id", nullable = false, updatable = false)
    private UUID enquiryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public static NotificationTask pendingEmailFor(UUID enquiryId) {
        NotificationTask task = new NotificationTask();
        Instant now = Instant.now();
        task.enquiryId = enquiryId;
        task.channel = NotificationChannel.EMAIL;
        task.status = NotificationStatus.PENDING;
        task.attemptCount = 0;
        task.nextAttemptAt = now;
        task.createdAt = now;
        return task;
    }
}
