package com.vipa.medllm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.vipa.medllm.dto.middto.task.TaskProcessDto;
import com.vipa.medllm.dto.response.ResponseResult;
import com.vipa.medllm.model.LLMTaskType;
import com.vipa.medllm.model.Project;
import com.vipa.medllm.service.task.TaskService;

import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Controller
@AllArgsConstructor
@RequestMapping("/task")
public class TaskContoller {

    private final TaskService taskService;
    private static final String TASK_PROGRESS_TOPIC = "/topic/task_progress/";
    private SimpMessagingTemplate messagingTemplate;

    public void sendTaskProgress(String taskId, TaskProcessDto taskProcessDto) {
        // 使用SimpMessagingTemplate将消息发送到指定的主题
        messagingTemplate.convertAndSend(TASK_PROGRESS_TOPIC + taskId, taskProcessDto);
    }

    @GetMapping("/searchLLMTaskType")
    public ResponseEntity<ResponseResult<List<LLMTaskType>>> searchLLMTaskType(
            @RequestParam(required = false) Integer llmTaskTypeId,
            @RequestParam(required = false) Boolean isPreProcessTask,
            @RequestParam(required = false) String llmTaskTypeName, @RequestParam(required = false) String prompt,
            @RequestParam(required = false) String description) {

        List<LLMTaskType> llmTaskTypes = taskService.searchLLMTaskType(llmTaskTypeId, isPreProcessTask, llmTaskTypeName,
                prompt, description);

        ResponseResult<List<LLMTaskType>> response = new ResponseResult<>(200, "Project search successfully",
                llmTaskTypes);

        return ResponseEntity.ok(response);
    }

}
