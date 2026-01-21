package com.ebbinghaus.memory.app.model;

import lombok.Data;

import java.util.List;

@Data
public class ExplainItBackFeedback {
  private Integer score;
  private String summary;
  private List<String> missingPoints;
  private List<String> improvements;
}
