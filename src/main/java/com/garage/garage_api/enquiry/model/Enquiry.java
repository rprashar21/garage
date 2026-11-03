package com.garage.garage_api.enquiry.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enquiry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "submission_key", nullable = false, unique = true)
    private UUID submissionKey;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_contact", nullable = false, length = 20)
    private PreferredContact preferredContact;

    @Enumerated(EnumType.STRING)
    @Column(name = "enquiry_type", nullable = false, length = 30)
    private EnquiryType enquiryType;

    @Column(name = "vehicle_make", length = 60)
    private String vehicleMake;

    @Column(name = "vehicle_model", length = 80)
    private String vehicleModel;

    @Column(name = "vehicle_year")
    private Short vehicleYear;

    @Column(length = 20)
    private String registration;

    @Column(nullable = false, length = 4000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnquiryStatus status = EnquiryStatus.NEW;

    @Column(name = "privacy_notice_version", nullable = false, length = 30)
    private String privacyNoticeVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    //@PrePersist and @PreUpdate are JPA lifecycle callbacks:
    // JPA automatically calls these methods before certain database operations

    //when we change an existing enquiry—for example,
    //its status from NEW to CONTACTED—this refreshes updatedAt before the UPDATE. createdAt stays unchanged.
    //New enquiry inserted:
    //New enquiry inserted:
    //createdAt = Monday 10:00
    //updatedAt = Monday 10:00
    //
    //Status changed on Tuesday:
    //createdAt = Monday 10:00
    //updatedAt = Tuesday 14:30
    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }
}
