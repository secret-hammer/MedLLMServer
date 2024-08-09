package com.vipa.medllm.service;

import com.vipa.medllm.config.redisconfig.RedisCache;
import com.vipa.medllm.dto.middto.task.CreatePathologyImageConvertTaskDto;
import com.vipa.medllm.dto.middto.task.TaskProcessDto;
import com.vipa.medllm.service.task.TaskService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TaskServiceTest {

    private static final String TASK_PROGRESS_CACHE_KEY = "task_progress";

    private RedisCache redisCache;
    private TaskService taskService;

    @Autowired
    public TaskServiceTest(RedisCache redisCache, TaskService taskService) {
        this.redisCache = redisCache;
        this.taskService = taskService;
    }

    @Test
    public void testSubmitPathologyImageConvertTask() {
        Integer projectId = 1;
        Integer imageId = 1;
        String imageUrl = "http://localhost:8080/image/1.jpg";

        CreatePathologyImageConvertTaskDto createTaskDto = new CreatePathologyImageConvertTaskDto(projectId,
                imageId,
                imageUrl);

        String taskId = taskService.submitPathologyImageConvertTask(createTaskDto);

        TaskProcessDto taskProcessDto = redisCache.<TaskProcessDto>getCacheMapValue(TASK_PROGRESS_CACHE_KEY,
                taskId, TaskProcessDto.class);

        System.out.println(taskId);
        System.out.println(taskProcessDto);

        // 验证Redis中的数据（示例：验证状态是否为初始值0）
        assertEquals(0, taskProcessDto.getStatus());
    }

}
