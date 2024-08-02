package com.vipa.medllm.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ResponseGetGroups {
    private List<GroupDTO> groups;

    @Data
    @AllArgsConstructor
    public static class GroupDTO {
        private int imageGroupId;
        private String imageGroupName;
        private String description;
        private int projectId;
    }
}
