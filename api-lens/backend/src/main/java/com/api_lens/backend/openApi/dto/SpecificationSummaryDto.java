package com.api_lens.backend.openApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpecificationSummaryDto {
    private String fileName;
    private String title;
    private SpecStatus status;
    private Instant uploadAt;
}
