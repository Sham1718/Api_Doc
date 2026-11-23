package com.api_lens.backend.openApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpecificationSummaryDto {
    private String id;
    private String fileName;
    private String openAPiVersion;
    private String title;
    private int pathCount;
    private SpecStatus status;
    private Instant uploadedAt;
    private List<EndpointDto> endpointDtoList;
}
