package com.medianet.reservation.controller;

import com.medianet.reservation.dto.ChatbotRequest;
import com.medianet.reservation.dto.ChatbotResponse;
import com.medianet.reservation.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint du chatbot. Accessible a tout utilisateur authentifie (employe ou administrateur) :
 * seul /api/admin/** est restreint aux administrateurs dans SecurityConfig, donc /api/chatbot/**
 * retombe sur la regle generale ".anyRequest().authenticated()".
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ChatbotResponse demander(@Valid @RequestBody ChatbotRequest request) {
        return chatbotService.repondre(request.message());
    }
}
