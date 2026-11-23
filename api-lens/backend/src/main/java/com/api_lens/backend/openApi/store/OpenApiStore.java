package com.api_lens.backend.openApi.store;

import com.api_lens.backend.openApi.dto.EndpointDto;
import com.api_lens.backend.openApi.dto.SpecificationDto;
import com.api_lens.backend.openApi.dto.SpecificationSummaryDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OpenApiStore {
    ConcurrentHashMap<String,SpecificationDto> store =new ConcurrentHashMap<>();
    public SpecificationSummaryDto storeDoc(SpecificationDto file, List<EndpointDto> endpointDto) {
      store.put(file.getId(), file);
      System.out.println(file.getRawContent());
      return new SpecificationSummaryDto(file.getId(),file.getFileName(),file.getApiVersion(), file.getTitle(),file.getPathCount(),
              file.getStatus(),file.getUploadAt(),endpointDto);
    }
}
