package com.api_lens.backend.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class AiServiceClient {
    private final RestClient restClient;


    public AiServiceClient(RestClient.Builder builder,
                           @Value("${AI.SERVICE.BASE_URL}") String baseUrl) {

        this.restClient = builder.baseUrl(baseUrl).build();
    }
    public Map<String,String> health(){
        return restClient.get()
                .uri("/internal/v1/health")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, String>>() {});
    }


}
