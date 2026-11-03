package com.api_lens.backend.openApi.store;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OpenApiStore {
    ConcurrentHashMap<Integer,MultipartFile> store =new ConcurrentHashMap<>();
    public void storeDoc(MultipartFile file) throws IOException {
        String content=new String(file.getBytes(), StandardCharsets.UTF_8);
        store.put(1,file);
        System.out.println(content);
    }
}
