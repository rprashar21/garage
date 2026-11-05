package com.garage.garage_api.enquiry.service;

import com.garage.garage_api.enquiry.dto.EnquiryRequest;
import com.garage.garage_api.enquiry.dto.EnquiryResponse;
import com.garage.garage_api.enquiry.model.Enquiry;
import com.garage.garage_api.enquiry.model.EnquiryStatus;
import com.garage.garage_api.enquiry.model.EnquiryType;
import com.garage.garage_api.enquiry.model.PreferredContact;
import com.garage.garage_api.enquiry.repository.EnquiryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnquiryServiceTest {

    private static final UUID KEY = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private EnquiryRepository enquiryRepository;

    @Mock
    private EnquiryCreator enquiryCreator;

    @InjectMocks
    private EnquiryService enquiryService;

    private EnquiryRequest request(String name, String email, String phone, String vehicleMake, String message) {
        return new EnquiryRequest(KEY, name, email, phone, PreferredContact.EMAIL, EnquiryType.SERVICE,
                vehicleMake, null, null, null, message, "v1");
    }

    private Enquiry enquiry(String reference) {
        return Enquiry.builder()
                .reference(reference)
                .submissionKey(KEY)
                .customerName("Sam")
                .email("sam@example.com")
                .preferredContact(PreferredContact.EMAIL)
                .enquiryType(EnquiryType.SERVICE)
                .message("Hi")
                .privacyNoticeVersion("v1")
                .build();
    }

    @Test
    void newEnquiryIsSavedAndReferenceReturned() {
        when(enquiryRepository.findBySubmissionKey(KEY)).thenReturn(Optional.empty());
        when(enquiryCreator.create(any(Enquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        EnquiryResponse response = enquiryService.submitEnquiry(
                request("Sam", "sam@example.com", null, "BMW", "Need a service"));

        assertThat(response.reference()).matches("ENQ-[0-9A-F]{8}");
        assertThat(response.status()).isEqualTo(EnquiryStatus.NEW);
        verify(enquiryCreator).create(any(Enquiry.class));
    }

    @Test
    void trimsTextAndTurnsBlankOptionalFieldsIntoNull() {
        when(enquiryRepository.findBySubmissionKey(KEY)).thenReturn(Optional.empty());
        when(enquiryCreator.create(any(Enquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        enquiryService.submitEnquiry(request("  Sam  ", "sam@example.com", "   ", "", "  Need a service  "));

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryCreator).create(captor.capture());
        Enquiry saved = captor.getValue();
        assertThat(saved.getCustomerName()).isEqualTo("Sam");
        assertThat(saved.getMessage()).isEqualTo("Need a service");
        assertThat(saved.getPhone()).isNull();
        assertThat(saved.getVehicleMake()).isNull();
        assertThat(saved.getSubmissionKey()).isEqualTo(KEY);
    }

    @Test
    void existingSubmissionKeyReturnsOriginalWithoutSaving() {
        when(enquiryRepository.findBySubmissionKey(KEY)).thenReturn(Optional.of(enquiry("ENQ-EXISTING")));

        EnquiryResponse response = enquiryService.submitEnquiry(
                request("Sam", "sam@example.com", null, null, "Hi"));

        assertThat(response.reference()).isEqualTo("ENQ-EXISTING");
        verify(enquiryCreator, never()).create(any());
    }

    @Test
    void duplicateKeyRaceReturnsTheWinnersEnquiry() {
        // First lookup: not found. Save: loses the race. Second lookup: the winner's row.
        when(enquiryRepository.findBySubmissionKey(KEY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(enquiry("ENQ-WINNER")));
        when(enquiryCreator.create(any(Enquiry.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        EnquiryResponse response = enquiryService.submitEnquiry(
                request("Sam", "sam@example.com", null, null, "Hi"));

        assertThat(response.reference()).isEqualTo("ENQ-WINNER");
        verify(enquiryRepository, times(2)).findBySubmissionKey(KEY);
    }

    @Test
    void integrityErrorWithNoExistingRowIsRethrown() {
        // e.g. a different constraint failed, so there is no winner to return.
        when(enquiryRepository.findBySubmissionKey(KEY)).thenReturn(Optional.empty());
        when(enquiryCreator.create(any(Enquiry.class)))
                .thenThrow(new DataIntegrityViolationException("some other constraint"));

        assertThatThrownBy(() -> enquiryService.submitEnquiry(
                request("Sam", "sam@example.com", null, null, "Hi")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
