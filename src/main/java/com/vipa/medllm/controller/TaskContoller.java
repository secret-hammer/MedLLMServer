package com.vipa.medllm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.vipa.medllm.dto.middto.task.TaskProcessDto;
import com.vipa.medllm.dto.request.task.LiveQARequest;
import com.vipa.medllm.dto.request.task.SearchLLMTaskTypeRequest;
import com.vipa.medllm.dto.response.ResponseResult;
import com.vipa.medllm.model.LLMTaskType;
import com.vipa.medllm.model.Project;
import com.vipa.medllm.service.task.TaskService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Controller
@AllArgsConstructor
@RequestMapping("/task")
public class TaskContoller {

    private final TaskService taskService;

    @GetMapping("/searchLLMTaskType")
    public ResponseEntity<ResponseResult<List<LLMTaskType>>> searchLLMTaskType(
            @RequestParam(required = false) Integer llmTaskTypeId,
            @RequestParam(required = false) Boolean isPreProcessTask,
            @RequestParam(required = false) String llmTaskTypeName, @RequestParam(required = false) String prompt,
            @RequestParam(required = false) String description) {

        SearchLLMTaskTypeRequest searchLLMTaskTypeRequest = new SearchLLMTaskTypeRequest(llmTaskTypeId,
                isPreProcessTask,
                llmTaskTypeName, prompt, description);
        List<LLMTaskType> llmTaskTypes = taskService.searchLLMTaskType(searchLLMTaskTypeRequest);

        ResponseResult<List<LLMTaskType>> response = new ResponseResult<>(200, "Project search successfully",
                llmTaskTypes);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/liveQA")
    public ResponseEntity<ResponseResult<String>> liveQA(@Valid @RequestBody LiveQARequest liveQaRequest) {
        String answer = taskService.liveQA(liveQaRequest);
        ResponseResult<String> response = new ResponseResult<>(200, "Live QA successfully", answer);
        return ResponseEntity.ok(response);
    }

}
