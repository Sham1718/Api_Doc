package com.api_lens.backend.openApi.store;

import com.api_lens.backend.openApi.dto.EndpointDto;

import java.util.concurrent.ConcurrentHashMap;

public class PathStore {
    ConcurrentHashMap<String, EndpointDto> endPoints=new ConcurrentHashMap<>();
    public void storeEndPoint(){

    }

}
