package com.vipa.medllm.controller;

import com.vipa.medllm.dto.request.group.DeleteGroupRequest;
import com.vipa.medllm.dto.request.group.DeleteProjectRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vipa.medllm.dto.request.project.CreateProjectInfo;
import com.vipa.medllm.dto.request.project.UpdateProjectInfo;
import com.vipa.medllm.dto.response.ResponseResult;
import com.vipa.medllm.model.Project;
import com.vipa.medllm.service.project.ProjectService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@AllArgsConstructor
@RequestMapping("/project")
public class ProjectController {

    private ProjectService projectService;

    @PostMapping("/create")
    public ResponseEntity<ResponseResult<Object>> createProjects(
            @Valid @RequestBody List<CreateProjectInfo> projectInfoList) {

        // 批量创建数据集
        projectService.createProjects(projectInfoList);

        ResponseResult<Object> response = new ResponseResult<>(200, "Project created successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ResponseResult<Object>> updateProjects(
            @Valid @RequestBody List<UpdateProjectInfo> projectInfoList) {

        // 批量更新数据集
        projectService.updateProjects(projectInfoList);

        ResponseResult<Object> response = new ResponseResult<>(200, "Projects updated successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseResult<List<Project>>> searchProjects(
            @RequestParam(required = false) Integer projectId, @RequestParam(required = false) String projectName) {

        List<Project> projects = projectService.searchProjects(projectId, projectName);

        ResponseResult<List<Project>> response = new ResponseResult<>(200, "Project search successfully", projects);

        return ResponseEntity.ok(response);
    }

    // @PostMapping("/delete")
    // public ResponseEntity<ResponseResult<List<String>>> deleteGroups(@RequestBody DeleteProjectRequest deleteProjectRequest) {

    //     projectService.deleteProject(deleteProjectRequest.getProjectId());
    //     ResponseResult<List<String>> response = new ResponseResult<>(200, "Group delete process completed");

    //     return ResponseEntity.ok(response);
    // }

    // @PostMapping("/delete")
    // public ResponseEntity<ResponseResult<Object>> deleteProjects(@RequestParam
    // List<Integer> projectIds) {
    // // 待开发

    // }
}
