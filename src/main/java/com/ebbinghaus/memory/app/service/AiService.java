package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.model.AiQuestionTuple;

public interface AiService {
    AiQuestionTuple sendRequest(String text, String languageCode);
}
