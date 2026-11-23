package com.api_lens.backend.openApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EndpointDto {
    private String path;
    private String httpMethod;
    private String operationId;
    private String summary;


}
