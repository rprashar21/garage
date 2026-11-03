package com.garage.garage_api.enquiry.dto;

import com.garage.garage_api.enquiry.model.EnquiryStatus;

import java.time.Instant;

public record EnquiryResponse(String reference, EnquiryStatus status, Instant createdAt) {
}
