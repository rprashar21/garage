package com.garage.garage_api.enquiry.controller;

import com.garage.garage_api.enquiry.dto.EnquiryRequest;
import com.garage.garage_api.enquiry.dto.EnquiryResponse;
import com.garage.garage_api.enquiry.service.EnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enquiries")
@RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnquiryResponse submitEnquiry(@Valid @RequestBody EnquiryRequest request) {
        return enquiryService.submitEnquiry(request);
    }
}
