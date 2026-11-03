package com.garage.garage_api.enquiry.controller;

import com.garage.garage_api.enquiry.dto.EnquiryRequest;
import com.garage.garage_api.enquiry.dto.EnquiryResponse;
import com.garage.garage_api.enquiry.model.EnquiryStatus;
import com.garage.garage_api.enquiry.service.EnquiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnquiryController.class)
class EnquiryControllerTest {

    private static final String URL = "/api/v1/enquiries";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnquiryService enquiryService;

    private static String json(String preferredContact, String email, String phone, String message) {
        return """
                {
                  "submissionKey": "11111111-1111-1111-1111-111111111111",
                  "customerName": "Sam",
                  "email": %s,
                  "phone": %s,
                  "preferredContact": "%s",
                  "enquiryType": "SERVICE",
                  "message": "%s",
                  "privacyNoticeVersion": "v1"
                }
                """.formatted(quote(email), quote(phone), preferredContact, message);
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    @Test
    void validRequestReturns201WithReference() throws Exception {
        when(enquiryService.submitEnquiry(any(EnquiryRequest.class)))
                .thenReturn(new EnquiryResponse("ENQ-ABCD1234", EnquiryStatus.NEW, Instant.parse("2026-11-03T10:00:00Z")));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("EMAIL", "sam@example.com", null, "Need a service")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("ENQ-ABCD1234"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void blankMessageReturns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("EMAIL", "sam@example.com", null, " ")))
                .andExpect(status().isBadRequest());

        verify(enquiryService, never()).submitEnquiry(any());
    }

    @Test
    void phonePreferredWithoutPhoneReturns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("PHONE", "sam@example.com", null, "Need a service")))
                .andExpect(status().isBadRequest());

        verify(enquiryService, never()).submitEnquiry(any());
    }

    @Test
    void emailPreferredWithBlankEmailReturns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("EMAIL", " ", "07123456789", "Need a service")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidEmailFormatReturns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("EMAIL", "not-an-email", null, "Need a service")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingSubmissionKeyReturns400() throws Exception {
        String body = """
                {"customerName":"Sam","email":"sam@example.com","preferredContact":"EMAIL",
                 "enquiryType":"SERVICE","message":"Hi","privacyNoticeVersion":"v1"}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEnquiryTypeReturns400() throws Exception {
        String body = json("EMAIL", "sam@example.com", null, "Hi").replace("SERVICE", "BOGUS");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
