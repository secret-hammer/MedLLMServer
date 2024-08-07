package com.vipa.medllm.dto.request.group;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeleteGroupRequest {
    @NotNull(message = "Group ID cannot be null")
    private Integer groupId;
}
