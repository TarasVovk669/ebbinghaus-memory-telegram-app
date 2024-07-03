package com.ebbinghaus.memory.app.model;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.domain.EMessage;
import java.util.Collection;

public record MessageTuple(EMessage message, Collection<Category> categories) {}
