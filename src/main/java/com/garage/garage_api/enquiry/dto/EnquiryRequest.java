package com.garage.garage_api.enquiry.dto;

import com.garage.garage_api.enquiry.model.EnquiryType;
import com.garage.garage_api.enquiry.model.PreferredContact;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record EnquiryRequest(
        @NotNull UUID submissionKey,
        @NotBlank @Size(max = 120) String customerName,
        @Email @Size(max = 254) String email,
        @Size(max = 32) String phone,
        @NotNull PreferredContact preferredContact,
        @NotNull EnquiryType enquiryType,
        @Size(max = 60) String vehicleMake,
        @Size(max = 80) String vehicleModel,
        @Min(1900) @Max(2100) Short vehicleYear,
        @Size(max = 20) String registration,
        @NotBlank @Size(max = 4000) String message,
        @NotBlank @Size(max = 30) String privacyNoticeVersion
) {

    // Mirrors chk_enquiry_contact_details so the client gets a 400 instead of a DB error.
    @AssertTrue(message = "Provide the phone or email that matches the preferred contact method")
    public boolean isPreferredContactDetailProvided() {
        if (preferredContact == null) {
            return true; // @NotNull already reports this
        }
        String detail = preferredContact == PreferredContact.PHONE ? phone : email;
        return detail != null && !detail.isBlank();
    }
}
