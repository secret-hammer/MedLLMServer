package com.vipa.medllm.controller;

import com.vipa.medllm.dto.request.group.CreateGroupRequest;
import com.vipa.medllm.dto.request.group.UpdateGroupRequest;
import com.vipa.medllm.dto.response.ResponseResult;
import com.vipa.medllm.model.ImageGroup;
import com.vipa.medllm.service.group.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/group")
public class GroupController {
    private GroupService groupService;

    @GetMapping("/search")
    public ResponseEntity<ResponseResult<List<ImageGroup>>> searchGroup(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String groupDescription) {

        List<ImageGroup> groups = groupService.searchGroup(projectId, groupId, groupName, groupDescription);
        ResponseResult<List<ImageGroup>> response = new ResponseResult<>(200,
                "Group information retrieved successfully", groups);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<ResponseResult<Object>> createGroup(
            @Valid @RequestBody CreateGroupRequest createGroupRequest) {

        groupService.createGroup(createGroupRequest);
        ResponseResult<Object> response = new ResponseResult<>(200, "Group created successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ResponseResult<Object>> updateGroup(
            @Valid @RequestBody UpdateGroupRequest updateGroupRequest) {

        groupService.updateGroup(updateGroupRequest);
        ResponseResult<Object> response = new ResponseResult<>(200, "Groups updated successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    public ResponseEntity<ResponseResult<Object>> deleteGroups(
            @Valid @NotNull @RequestBody Integer groupId) {

        groupService.deleteGroup(groupId);
        ResponseResult<Object> response = new ResponseResult<>(200, "Group delete process completed");

        return ResponseEntity.ok(response);
    }
}
