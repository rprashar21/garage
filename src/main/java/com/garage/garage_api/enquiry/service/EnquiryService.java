package com.garage.garage_api.enquiry.service;

import com.garage.garage_api.enquiry.dto.EnquiryRequest;
import com.garage.garage_api.enquiry.dto.EnquiryResponse;
import com.garage.garage_api.enquiry.model.Enquiry;
import com.garage.garage_api.enquiry.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;

    // Deliberately not @Transactional: save() runs in its own transaction, so a
    // duplicate-key failure surfaces here and can be handled.
    public EnquiryResponse submitEnquiry(EnquiryRequest request) {
        // Idempotency: a retried submit with the same key returns the original enquiry.
        return enquiryRepository.findBySubmissionKey(request.submissionKey())
                .map(this::toResponse)
                .orElseGet(() -> create(request));
    }

    private EnquiryResponse create(EnquiryRequest request) {
        Enquiry enquiry = Enquiry.builder()
                .reference(newReference())
                .submissionKey(request.submissionKey())
                .customerName(request.customerName().trim())
                .email(blankToNull(request.email()))
                .phone(blankToNull(request.phone()))
                .preferredContact(request.preferredContact())
                .enquiryType(request.enquiryType())
                .vehicleMake(blankToNull(request.vehicleMake()))
                .vehicleModel(blankToNull(request.vehicleModel()))
                .vehicleYear(request.vehicleYear())
                .registration(blankToNull(request.registration()))
                .message(request.message().trim())
                .privacyNoticeVersion(request.privacyNoticeVersion())
                .build();
        try {
            return toResponse(enquiryRepository.save(enquiry));
        } catch (DataIntegrityViolationException e) {
            // Two requests with the same key raced past the lookup above.
            return enquiryRepository.findBySubmissionKey(request.submissionKey())
                    .map(this::toResponse)
                    .orElseThrow(() -> e);
        }
    }

    // TODO: once saved, notify the mechanic (email/push). Still to be decided.

    private EnquiryResponse toResponse(Enquiry enquiry) {
        return new EnquiryResponse(enquiry.getReference(), enquiry.getStatus(), enquiry.getCreatedAt());
    }

    private String newReference() {
        return "ENQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
