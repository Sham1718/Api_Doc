package com.api_lens.backend.openApi.store;

import com.api_lens.backend.openApi.dto.EndpointDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PathStore {
    ConcurrentHashMap<String, EndpointDto> endPoints=new ConcurrentHashMap<>();
    public List<EndpointDto> storeEndPoint(List<EndpointDto> endpointDtos){
//        endpointDtos.forEach(endpointDto -> System.out.println(endpointDtos));
        return endpointDtos;
    }

}
