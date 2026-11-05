package com.api_lens.backend.openApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class SpecificationDto {
    private String id;
    private String fileName;
    private long sizeBytes;
    private SpecFormat format;
    private String rawContent;
    private SpecStatus status;
    private String openAPiVersion;
    private String title;
    private String apiVersion;
    private int pathCount;
    private int operationCount;
    private int schemeCount;
    private List<String> message;
    Instant uploadAt;
}
