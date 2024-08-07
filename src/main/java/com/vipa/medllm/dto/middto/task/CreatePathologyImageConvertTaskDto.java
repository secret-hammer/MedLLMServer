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

    @NotNull(message = "Project name cannot be null")
    private String projectName;

    @NotNull(message = "ImageId cannot be null")
    private Integer imageId;

    @NotNull(message = "Image name cannot be null")
    private String imageName;

    @NotNull(message = "Image URL cannot be null")
    private String imageUrl;

    public CreatePathologyImageConvertTaskDto(Project project, Image image) {
        this.projectId = project.getProjectId();
        this.projectName = project.getProjectName();
        this.imageId = image.getImageId();
        this.imageName = image.getImageName();
        this.imageUrl = image.getImageUrl();
    }

    public CreatePathologyImageConvertTaskDto(Integer projectId, String projectName, Integer imageId, String imageName,
            String imageUrl) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.imageId = imageId;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
    }
}
