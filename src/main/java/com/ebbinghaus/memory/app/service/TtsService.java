package com.ebbinghaus.memory.app.service;

import java.io.IOException;

public interface TtsService {
    byte[] synthesize(String text);
}
