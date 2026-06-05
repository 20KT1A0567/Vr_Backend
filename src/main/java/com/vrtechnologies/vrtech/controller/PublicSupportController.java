package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.SupportChatRequest;
import com.vrtechnologies.vrtech.dto.SupportChatResponse;
import com.vrtechnologies.vrtech.service.SupportChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/support")
@RequiredArgsConstructor
public class PublicSupportController {

    private final SupportChatService supportChatService;

    @PostMapping("/chat")
    public ResponseEntity<SupportChatResponse> handleChat(@RequestBody SupportChatRequest request) {
        SupportChatResponse response = supportChatService.handleChat(request);
        return ResponseEntity.ok(response);
    }
}
