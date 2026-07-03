package com.example.individuell_labb_2k5_robin.controller;

import com.example.individuell_labb_2k5_robin.dto.AiResponseDto;
import com.example.individuell_labb_2k5_robin.service.AiClientService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiClientService aiClientService;

    public AiController(AiClientService aiClientService) {
        this.aiClientService = aiClientService;
    }

    @PostMapping("/analyze")
    public AiResponseDto analyze(@RequestBody String text) {
        return aiClientService.analyzeText(text);
    }
}
