package com.api_lens.backend.openApi.store;

import com.api_lens.backend.openApi.dto.SpecificationDto;
import com.api_lens.backend.openApi.dto.SpecificationSummaryDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OpenApiStore {
    ConcurrentHashMap<String,SpecificationDto> store =new ConcurrentHashMap<>();
    public SpecificationSummaryDto storeDoc(SpecificationDto file) throws IOException {
      store.put(file.getId(), file);
      return new SpecificationSummaryDto(file.getFileName(), file.getTitle(), file.getStatus(),file.getUploadAt());
    }
}
