package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.service.AiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);


    @Override
    public String sendRequest() {
        return null;
    }
}
