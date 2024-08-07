package com.vipa.medllm.dto.request.task;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LiveQARequest {
    @NotNull(message = "LLMTaskTypeId cannot be null")
    private Integer llmTaskTypeId;

    @NotNull(message = "ImageId cannot be null")
    private Integer imageId;

    private String question;

    // 切割窗口的左上角横坐标
    private Float x;

    // 切割窗口的左上角纵坐标
    private Float y;

    // 切割窗口的宽度
    private Float width;

    // 切割窗口的高度
    private Float height;
}