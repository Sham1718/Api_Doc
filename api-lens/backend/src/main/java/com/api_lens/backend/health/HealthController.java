package com.api_lens.backend.health;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@CrossOrigin("http://localhost:5173/")
public class HealthController {
    @GetMapping("/")
    public Map<String,String> health(){
        return Map.of("Status","Up");
    }
}
