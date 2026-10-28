package com.api_lens.backend.ai.controller;

import com.api_lens.backend.ai.service.AiServiceClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiServiceController {
    private final AiServiceClient aiServiceClient;

    public AiServiceController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }
    @GetMapping("/health")
    public Map<String,String> health(){
        return aiServiceClient.health();
    }
}
