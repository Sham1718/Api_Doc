package com.api_lens.backend.openApi.service;

import com.api_lens.backend.openApi.dto.*;
import com.api_lens.backend.openApi.store.OpenApiStore;
import com.api_lens.backend.openApi.store.PathStore;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SpecificationService {
    private final OpenApiStore store;
    private final PathStore pathStore;

    public SpecificationService(OpenApiStore store, PathStore pathStore) {
        this.store = store;
        this.pathStore = pathStore;
    }


    public SpecificationSummaryDto uploadDoc(MultipartFile file) throws IOException {
        String content=new String(file.getBytes(), StandardCharsets.UTF_8);
        SwaggerParseResult result=new OpenAPIParser().readContents(content,null,null);
        List<String>message=result.getMessages()==null?List.of():result.getMessages();
        SpecFormat format=content.stripLeading().startsWith("{")?SpecFormat.JSON:SpecFormat.YAML;
        OpenAPI api=result.getOpenAPI();
        SpecificationDto dto;
        List<EndpointDto> endpointDto=getEndpoints(api);

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
        return store.storeDoc(dto,endpointDto);

    }

    private List<EndpointDto> getEndpoints(OpenAPI api){
        Paths paths=api.getPaths();
        List<EndpointDto> endpointDtos=new ArrayList<>();

        if(paths!=null){
            for(Map.Entry<String , PathItem> pathItemEntry: paths.entrySet()){
                String path=pathItemEntry.getKey();
                PathItem pathItem=pathItemEntry.getValue();

                Map<PathItem.HttpMethod,Operation> operationMap =pathItem.readOperationsMap();
                for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : operationMap.entrySet()){
                    String method=operationEntry.getKey().name();
                    Operation operation=operationEntry.getValue();

                    EndpointDto dto=new EndpointDto();
                    dto.setPath(path);
                    dto.setHttpMethod(method);
                    dto.setOperationId(operation.getOperationId());
                    dto.setSummary(operation.getSummary());

                    endpointDtos.add(dto);
                }
            };
        }
        return pathStore.storeEndPoint(endpointDtos);
    }
    //specification/: version policy, Swagger Parser adapter, parse result, diagnostics.
    //export/: Markdown renderer and download response.
    //session/: in-memory bounded session store, TTL and expiry.
    //quality/: deterministic checks and warning model.
}
