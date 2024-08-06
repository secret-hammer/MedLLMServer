package com.vipa.medllm.controller;

import com.vipa.medllm.dto.request.group.CreateGroupRequest;
import com.vipa.medllm.dto.request.group.DeleteGroupRequest;
import com.vipa.medllm.dto.request.group.UpdateGroupRequest;
import com.vipa.medllm.dto.response.ResponseResult;
import com.vipa.medllm.service.group.GroupService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ResponseResult<Object>> searchGroup(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String groupDescription) {

        Object groupInfo = groupService.searchGroup(projectId, groupId, groupName, groupDescription);
        ResponseResult<Object> response = new ResponseResult<>(200, "Group information retrieved successfully", groupInfo);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<ResponseResult<Object>> createGroup(@Valid @RequestBody CreateGroupRequest createGroupRequest) {

        groupService.createGroup(createGroupRequest);
        ResponseResult<Object> response = new ResponseResult<>(200, "Group created successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ResponseResult<Object>> updateGroup(@Valid @RequestBody UpdateGroupRequest updateGroupRequest) {

        groupService.updateGroup(updateGroupRequest);
        ResponseResult<Object> response = new ResponseResult<>(200, "Groups updated successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    public ResponseEntity<ResponseResult<List<String>>> deleteGroups(@RequestBody DeleteGroupRequest deleteGroupRequest) {

        groupService.deleteGroup(deleteGroupRequest);
        ResponseResult<List<String>> response = new ResponseResult<>(200, "Group delete process completed");

        return ResponseEntity.ok(response);
    }
}
