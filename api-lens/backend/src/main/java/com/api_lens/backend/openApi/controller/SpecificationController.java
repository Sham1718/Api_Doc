package com.api_lens.backend.openApi.controller;

import com.api_lens.backend.openApi.service.SpecificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@CrossOrigin("http://localhost:5173/")
@RestController
@RequestMapping("/api/v1/openApi")
public class SpecificationController {
    private final SpecificationService service;

    public SpecificationController(SpecificationService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadOpenApiDoc(@RequestParam("file")MultipartFile file) throws IOException {
        if (file.isEmpty()){
            return ResponseEntity.badRequest().body("Empty file");
        }
        service.uploadDoc(file);
        return ResponseEntity.ok("Document Uploaded");
    }
}
