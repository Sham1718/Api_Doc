package com.api_lens.backend.openApi.store;

import com.api_lens.backend.openApi.dto.SpecificationDto;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OpenApiStore {
    ConcurrentHashMap<String,SpecificationDto> store =new ConcurrentHashMap<>();
    public void storeDoc(SpecificationDto file) throws IOException {
      store.put(file.getId(), file);
        System.out.println(file);
    }
}
