package com.vipa.medllm.dto.middto.task;

import com.vipa.medllm.model.Image;
import com.vipa.medllm.model.LLMTaskType;
import com.vipa.medllm.model.Session;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatePathologyLLMInferenceTaskDto {
    // 由taskService创建并设置
    private String taskId;

    @NotNull(message = "Session cannot be null")
    private Session session;

    @NotNull(message = "Image Id cannot be null")
    private Integer imageId;

    @NotNull(message = "Image URL cannot be null")
    private String imageUrl;

    @NotNull(message = "LLM task type Id cannot be null")
    private Integer llmTaskTypeId;

    @NotNull(message = "Prompt cannot be null")
    private String prompt;

    public CreatePathologyLLMInferenceTaskDto(Session session, Image image, LLMTaskType llmTaskType) {
        this.session = session;
        this.imageId = image.getImageId();
        this.imageUrl = image.getImageUrl();
        this.llmTaskTypeId = llmTaskType.getLlmTaskTypeId();
        this.prompt = llmTaskType.getPrompt();
    }

    public CreatePathologyLLMInferenceTaskDto(Session session, Integer imageId, String imageUrl, Integer llmTaskTypeId,
            String prompt) {
        this.session = session;
        this.imageId = imageId;
        this.imageUrl = imageUrl;
        this.llmTaskTypeId = llmTaskTypeId;
        this.prompt = prompt;
    }
}
