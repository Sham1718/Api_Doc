package com.api_lens.backend.openApi.service;

import com.api_lens.backend.openApi.dto.SpecFormat;
import com.api_lens.backend.openApi.dto.SpecStatus;
import com.api_lens.backend.openApi.dto.SpecificationDto;
import com.api_lens.backend.openApi.store.OpenApiStore;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SpecificationService {
    private final OpenApiStore store;

    public SpecificationService(OpenApiStore store) {
        this.store = store;
    }


    public void uploadDoc(MultipartFile file) throws IOException {
        String content=new String(file.getBytes(), StandardCharsets.UTF_8);
        SwaggerParseResult result=new OpenAPIParser().readContents(content,null,null);
        List<String>message=result.getMessages()==null?List.of():result.getMessages();
        SpecFormat format=content.stripLeading().startsWith("{")?SpecFormat.JSON:SpecFormat.YAML;
        OpenAPI api=result.getOpenAPI();
        SpecificationDto dto;

        if(api==null){
            dto=new SpecificationDto(UUID.randomUUID().toString(),file.getOriginalFilename(), file.getSize(), format,content,
                    SpecStatus.Invalid,null,null,null,0,
                    0,0,message, Instant.now());
        } else {
            int path=api.getPaths()==null ? 0 : api.getPaths().size();
            int ops=api.getPaths() == null ? 0 : api.getPaths().values().stream()
                    .mapToInt(p-> p.readOperations().size()).sum();
            int schemas=(api.getComponents() == null || api.getComponents().getSchemas()==null) ? 0:
                    api.getComponents().getSchemas().size();
            dto =new SpecificationDto(UUID.randomUUID().toString(), file.getOriginalFilename(), file.getSize(), format,content,
                    SpecStatus.Parse,api.getOpenapi(),api.getInfo()!=null ?api.getInfo().getTitle() :null,
                    api.getInfo()!=null ? api.getInfo().getVersion() : null,path,ops,schemas,message,Instant.now() );
        }
        store.storeDoc(dto);

    }
    //specification/: version policy, Swagger Parser adapter, parse result, diagnostics.
    //export/: Markdown renderer and download response.
    //session/: in-memory bounded session store, TTL and expiry.
    //quality/: deterministic checks and warning model.
}
