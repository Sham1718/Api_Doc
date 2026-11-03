package com.api_lens.backend.openApi.service;

import com.api_lens.backend.openApi.store.OpenApiStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class SpecificationService {
    private final OpenApiStore store;

    public SpecificationService(OpenApiStore store) {
        this.store = store;
    }


    public void uploadDoc(MultipartFile file) throws IOException {
        store.storeDoc(file);

    }
    //specification/: version policy, Swagger Parser adapter, parse result, diagnostics.
    //export/: Markdown renderer and download response.
    //session/: in-memory bounded session store, TTL and expiry.
    //quality/: deterministic checks and warning model.
}
