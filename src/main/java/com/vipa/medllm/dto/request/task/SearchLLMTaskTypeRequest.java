package com.vipa.medllm.dto.request.task;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchLLMTaskTypeRequest {
    private Integer llmTaskTypeId;
    private Boolean isPreProcessTask;
    private String llmTaskTypeName;
    private String prompt;
    private String description;
}
