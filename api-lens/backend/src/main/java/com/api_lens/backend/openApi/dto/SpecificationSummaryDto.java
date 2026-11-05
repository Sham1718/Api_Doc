package com.api_lens.backend.openApi.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SpecificationSummaryDto {
    private String fileName;
    private String title;
    private SpecStatus status;
    private Instant uploadAt;
}
