package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.EnquiryRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.entity.Enquiry;
import com.vrtechnologies.vrtech.service.EnquiryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping
    public ApiResponse<Enquiry> create(@Valid @RequestBody EnquiryRequest request) {
        return ApiResponse.ok("Enquiry submitted", enquiryService.create(request));
    }
}
