package com.vipa.medllm.service.task;

import java.sql.Timestamp;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.vipa.medllm.config.redisconfig.RedisCache;
import com.vipa.medllm.controller.TaskContoller;
import com.vipa.medllm.dto.middto.task.TaskProcessDto;
import com.vipa.medllm.model.LLMTaskType;
import com.vipa.medllm.model.Project;
import com.vipa.medllm.model.QAPair;
import com.vipa.medllm.model.Session;
import com.vipa.medllm.repository.LLMTaskTypeRepository;
import com.vipa.medllm.repository.QAPairRepository;
import com.vipa.medllm.repository.SessionRepository;
import com.vipa.medllm.dto.middto.task.CreatePathologyImageConvertTaskDto;
import com.vipa.medllm.dto.middto.task.CreatePathologyLLMInferenceTaskDto;
import com.vipa.medllm.dto.middto.task.ImageConvertTaskCallbackDto;
import com.vipa.medllm.dto.middto.task.LLMInferenceTaskCallbackDto;
import com.vipa.medllm.dto.middto.session.CreateQAPairDto;
import com.vipa.medllm.service.session.QAPairService;

import io.jsonwebtoken.io.SerializationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private TaskContoller taskController;
    private RabbitTemplate rabbitTemplate;

    private RedisCache redisCache;

    private QAPairService qaPairService;

    private QAPairRepository qaPairRepository;
    private SessionRepository sessionRepository;
    private LLMTaskTypeRepository llmTaskTypeRepository;

    private static final String TASK_PROGRESS_CACHE_KEY = "task_progress";
    private static final String TASK_EXCHANGE = "task_exchange";
    private static final String PATHOLOGY_IMAGE_CONVERT_ROUTINGKEY = "pathology_image_convert";
    private static final String PATHOLOGY_LLM_INFERENCE_ROUTINGKEY = "pathology_llm_inference";

    @Value("${spring.application.name}")
    private String applicationName;

    public Integer submitPathologyImageConvertTask(
            @Valid CreatePathologyImageConvertTaskDto createPathologyImageConvertTaskDto)
            throws AmqpException, RedisConnectionFailureException, SerializationException {
        // 创建任务ID
        String taskId = String.format("%s_%s_%d", applicationName, PATHOLOGY_IMAGE_CONVERT_ROUTINGKEY,
                createPathologyImageConvertTaskDto.getImageId());

        createPathologyImageConvertTaskDto.setTaskId(taskId);
        // 创建病理图像转换任务消息
        rabbitTemplate.convertAndSend(TASK_EXCHANGE, PATHOLOGY_IMAGE_CONVERT_ROUTINGKEY,
                createPathologyImageConvertTaskDto);

        // 创建任务进度缓存
        redisCache.setCacheMapValue(TASK_PROGRESS_CACHE_KEY, taskId, new TaskProcessDto());

        return 0;
    }

    public void submitPathologyLLMInferenceTask(
            @Valid CreatePathologyLLMInferenceTaskDto createPathologyLLMInferenceTaskDto)
            throws AmqpException, RedisConnectionFailureException, SerializationException {
        // 创建任务ID
        String taskId = String.format("%s_%s_%d_%d", applicationName, PATHOLOGY_LLM_INFERENCE_ROUTINGKEY,
                createPathologyLLMInferenceTaskDto.getLlmTaskTypeId(),
                createPathologyLLMInferenceTaskDto.getImageId());

        createPathologyLLMInferenceTaskDto.setTaskId(taskId);
        // 创建病理LLM推理任务消息
        rabbitTemplate.convertAndSend(TASK_EXCHANGE, PATHOLOGY_LLM_INFERENCE_ROUTINGKEY,
                createPathologyLLMInferenceTaskDto);

        // 创建QAPair
        qaPairService.createQAPair(new CreateQAPairDto(createPathologyLLMInferenceTaskDto.getSession(),
                createPathologyLLMInferenceTaskDto.getLlmTaskTypeId(),
                createPathologyLLMInferenceTaskDto.getPrompt(),
                new Timestamp(System.currentTimeMillis())));

        // 创建任务进度缓存
        redisCache.setCacheMapValue(TASK_PROGRESS_CACHE_KEY, taskId, new TaskProcessDto());
    }

    public void imageConvertTaskFinishCallback(@Valid ImageConvertTaskCallbackDto imageConvertTaskCallbackDto) {
        String taskId = imageConvertTaskCallbackDto.getTaskId();
        TaskProcessDto taskProcessDto = redisCache.<TaskProcessDto>getCacheObject(taskId);

        // 任务还没有执行完毕，正常不会出现这种情况
        if (taskProcessDto.getStatus() == 0 || taskProcessDto.getStatus() == 1) {
            return;
        }

        if (taskProcessDto.getStatus() == 3) {
            log.error("imageConvertTaskFinishCallback: task failed, taskId: {}, message {}", taskId,
                    taskProcessDto.getMessage());
            return;
        }

        // 主动推送任务进度
        taskController.sendTaskProgress(taskId, taskProcessDto);
        // 删除任务进度缓存（任务失败仍会保留在里面，等待人工处理）
        redisCache.delCacheMapValue(TASK_PROGRESS_CACHE_KEY, taskId);

        Session session = sessionRepository.findByImageId(imageConvertTaskCallbackDto.getImageId());
        if (session == null) {
            log.error("imageConvertTaskFinishCallback: session not found, imageId: {}",
                    imageConvertTaskCallbackDto.getImageId());
            return;
        }
        // 根据session的状态执行状态转移
        sessionStatusTransferHandler(session, 1);
        sessionRepository.save(session);
    }

    public void llmInferenceTaskFinishCallback(@Valid LLMInferenceTaskCallbackDto llmInferenceTaskCallbackDto) {
        String taskId = llmInferenceTaskCallbackDto.getTaskId();
        TaskProcessDto taskProcessDto = redisCache.<TaskProcessDto>getCacheObject(taskId);

        // 任务还没有执行完毕，正常不会出现这种情况
        if (taskProcessDto.getStatus() == 0 || taskProcessDto.getStatus() == 1) {
            return;
        }

        if (taskProcessDto.getStatus() == 3) {
            log.error("llmInferenceTaskFinishCallback: task failed, taskId: {}, message {}",
                    taskId, taskProcessDto.getMessage());
            return;
        }
        // 主动推送任务进度
        taskController.sendTaskProgress(taskId, taskProcessDto);
        // 删除任务进度缓存（任务失败仍会保留在里面，等待人工处理）
        redisCache.delCacheMapValue(TASK_PROGRESS_CACHE_KEY, taskId);

        Session session = sessionRepository.findByImageId(llmInferenceTaskCallbackDto.getImageId());
        if (session == null) {
            log.error("llmInferenceTaskFinishCallback: session not found, imageId: {}",
                    llmInferenceTaskCallbackDto.getImageId());
            return;
        }

        QAPair qaPair = qaPairService.findQAPair(null, session.getSessionId(),
                llmInferenceTaskCallbackDto.getLlmTaskTypeId());

        qaPair.setAnswer(taskProcessDto.getMessage());
        qaPair.setAnswerTime(new Timestamp(System.currentTimeMillis()));
        qaPairRepository.save(qaPair);

        Map<Integer, QAPair> qaPairPreInferenceTaskMap = session.getQaPairPreInferenceTaskMap();
        qaPairPreInferenceTaskMap.put(llmInferenceTaskCallbackDto.getLlmTaskTypeId(), qaPair);

        // 如果所有预处理任务都执行完毕
        if (qaPairPreInferenceTaskMap.size() == llmTaskTypeRepository.countByIsPreProcessTask(true)) {
            // 根据session的状态执行状态转移
            sessionStatusTransferHandler(session, 2);
        }
        sessionRepository.save(session);
    }

    private void sessionStatusTransferHandler(Session session, Integer task) {
        Integer curStatus = session.getStatus();
        switch (task) {
            case 1:
                if (curStatus == 0) {
                    session.setStatus(2);
                } else if (curStatus == 1) {
                    session.setStatus(3);
                } else {
                    log.error(
                            "sessionStatusTransferHandler: invalid status or session is deprecated, sessionId: {}, status: {}",
                            session.getSessionId(), curStatus);
                }
                break;
            case 2:
                if (curStatus == 0) {
                    session.setStatus(1);
                } else if (curStatus == 2) {
                    session.setStatus(3);
                } else {
                    log.error(
                            "sessionStatusTransferHandler: invalid status or session is deprecated, sessionId: {}, status: {}",
                            session.getSessionId(), curStatus);
                }
                break;
            default:
                log.error("sessionStatusTransferHandler: invalid task, taskType: {}", task);
                break;
        }
    }

    // 每5秒检查一次任务进度
    @Scheduled(fixedRate = 5000)
    public void checkTaskProgress() {
        Map<String, TaskProcessDto> taskProgressMap = redisCache.getCacheMap(TASK_PROGRESS_CACHE_KEY);
        for (Map.Entry<String, TaskProcessDto> entry : taskProgressMap.entrySet()) {
            String taskId = entry.getKey();
            TaskProcessDto taskProcessDto = entry.getValue();
            taskController.sendTaskProgress(taskId, taskProcessDto);
        }
    }

    public List<LLMTaskType> searchLLMTaskType(Integer llmTaskTypeId, Boolean isPreProcessTask, String llmTaskTypeName,
            String prompt, String description) {
        Specification<LLMTaskType> spec = Specification.where(null);
        if (llmTaskTypeId != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("llmTaskTypeId"),
                    llmTaskTypeId));
        }
        if (isPreProcessTask != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isPreProcessTask"),
                    isPreProcessTask));
        }
        if (llmTaskTypeName != null && !llmTaskTypeName.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("llmTaskTypeName"),
                    "%" + llmTaskTypeName + "%"));
        }
        if (prompt != null && !prompt.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("prompt"),
                    "%" + prompt + "%"));
        }
        if (description != null && !description.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("description"),
                    "%" + description + "%"));
        }
        return llmTaskTypeRepository.findAll(spec);
    }
}
