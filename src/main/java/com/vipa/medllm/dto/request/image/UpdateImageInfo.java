package com.vipa.medllm.dto.request.image;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateImageInfo {
    @NotNull(message = "Image Id cannot be null")
    private Integer ImageId;

    private Integer newImageGroupId;

    private String newImageName;
}