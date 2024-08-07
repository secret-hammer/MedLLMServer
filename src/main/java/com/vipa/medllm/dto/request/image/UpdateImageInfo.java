package com.vipa.medllm.dto.request.image;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateImageRequest {
    @NotNull(message = "Image Id cannot be null")
    private int ImageId;

    private int newImageGroupId;

    private String newImageName;
}
