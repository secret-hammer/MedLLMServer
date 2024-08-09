package com.vipa.medllm.dto.middto.task;

import com.vipa.medllm.model.Image;
import com.vipa.medllm.model.Project;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatePathologyImageConvertTaskDto {

    // 由taskService创建并设置
    private String taskId;

    @NotNull(message = "ProjectId cannot be null")
    private Integer projectId;

    @NotNull(message = "ImageId cannot be null")
    private Integer imageId;

    @NotNull(message = "Image URL cannot be null")
    private String imageUrl;

    public CreatePathologyImageConvertTaskDto(Project project, Image image) {
        this.projectId = project.getProjectId();
        this.imageId = image.getImageId();
        this.imageUrl = image.getImageUrl();
    }

    public CreatePathologyImageConvertTaskDto(Integer projectId, Integer imageId, String imageUrl) {
        this.projectId = projectId;
        this.imageId = imageId;
        this.imageUrl = imageUrl;
    }
}
