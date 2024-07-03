package com.ebbinghaus.memory.app.model;

import java.util.List;
import lombok.Data;

@Data
public class QuestionsWrapper {
  private List<QuizQuestionDto> questions;
  private QuizManageStatus error;
}
