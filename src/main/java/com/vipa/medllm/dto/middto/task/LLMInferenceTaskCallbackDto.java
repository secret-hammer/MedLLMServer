package com.vipa.medllm.dto.middto.task;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LLMInferenceTaskCallbackDto {
    @NotNull(message = "Task ID is required")
    private String taskId;

    @NotNull(message = "LLM task type ID is required")
    private Integer llmTaskTypeId;

    @NotNull(message = "Image Id is required")
    private Integer imageId;
}
