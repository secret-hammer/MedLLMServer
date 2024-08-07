package com.vipa.medllm.dto.middto.task;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageConvertTaskCallbackDto {
    @NotNull(message = "Task ID is required")
    private String taskId;

    @NotNull(message = "Image ID is required")
    private Integer imageId;
}
