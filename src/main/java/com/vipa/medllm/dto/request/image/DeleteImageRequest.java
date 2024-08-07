package com.vipa.medllm.dto.request.image;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeleteImageRequest {
    @NotNull(message = "Image IDs cannot be null")
    private List<Integer> imageIds;
}
