package com.vipa.medllm.service.task;

import java.sql.Timestamp;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.vipa.medllm.config.redisconfig.RedisCache;
import com.vipa.medllm.controller.TaskContoller;
import com.vipa.medllm.dto.middto.task.TaskProcessDto;
import com.vipa.medllm.dto.request.task.LiveQARequest;
import com.vipa.medllm.dto.request.task.LiveQAToComputationRequest;
import com.vipa.medllm.dto.request.task.SearchLLMTaskTypeRequest;
import com.vipa.medllm.dto.response.task.LiveQAResponse;
import com.vipa.medllm.exception.CustomError;
import com.vipa.medllm.exception.CustomException;
import com.vipa.medllm.model.*;
import com.vipa.medllm.repository.ImageRepository;
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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {
    private final RabbitTemplate rabbitTemplate;

    private final RedisCache redisCache;

    private final QAPairService qaPairService;

    private final ImageRepository imageRepository;
    private final QAPairRepository qaPairRepository;
    private final SessionRepository sessionRepository;
    private final LLMTaskTypeRepository llmTaskTypeRepository;

    private final SimpMessagingTemplate messagingTemplate;

    private static final String IMAGE_CONVERT_TASK_PROGRESS_CACHE_KEY = "pathology_image_convert_task_progress";
    private static final String IMAGE_CONVERT_TASK_SUCCESS_CACHE_KEY = "pathology_image_convert_task_success";
    private static final String IMAGE_CONVERT_TASK_FAILED_CACHE_KEY = "pathology_image_convert_task_failed";
    private static final String LLM_INFERENCE_TASK_PROGRESS_CACHE_KEY = "pathology_llm_inference_task_progress";
    private static final String LLM_INFERENCE_TASK_SUCCESS_CACHE_KEY = "pathology_llm_inference_task_success";
    private static final String LLM_INFERENCE_TASK_FAILED_CACHE_KEY = "pathology_llm_inference_task_failed";


    private static final String TASK_EXCHANGE = "task_exchange";
    private static final String PATHOLOGY_IMAGE_CONVERT_ROUTINGKEY = "pathology_image_convert";
    private static final String PATHOLOGY_LLM_INFERENCE_ROUTINGKEY = "pathology_llm_inference";
    private static final String TASK_PROGRESS_TOPIC = "/topic/task_progress/";

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${computation.server.url}")
    private String computationServerUrl;

    public String submitPathologyImageConvertTask(
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
        redisCache.setCacheMapValue(IMAGE_CONVERT_TASK_PROGRESS_CACHE_KEY, taskId, new TaskProcessDto(createPathologyImageConvertTaskDto.getImageId()));

        return taskId;
    }

    public String submitPathologyLLMInferenceTask(
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
        redisCache.setCacheMapValue(LLM_INFERENCE_TASK_PROGRESS_CACHE_KEY, taskId, new TaskProcessDto(createPathologyLLMInferenceTaskDto.getImageId()));

        return taskId;
    }

    public void imageConvertTaskFinishCallback(@Valid ImageConvertTaskCallbackDto imageConvertTaskCallbackDto) {
        String taskId = imageConvertTaskCallbackDto.getTaskId();

        TaskProcessDto taskProcessDto = redisCache.<TaskProcessDto>getCacheMapValue(IMAGE_CONVERT_TASK_PROGRESS_CACHE_KEY, taskId,
                TaskProcessDto.class);

        Integer newStatus = imageConvertTaskCallbackDto.getStatus();
        Float newProgress = imageConvertTaskCallbackDto.getProgress();

        if(newStatus == 0 || newStatus == 1) {
            taskProcessDto.updateProgress(newStatus, newProgress);
            // 保存任务进度缓存
            redisCache.setCacheMapValue(IMAGE_CONVERT_TASK_PROGRESS_CACHE_KEY, taskId, taskProcessDto);
        } 
        else if(newStatus == 3) {
            taskProcessDto.updateResult(newStatus, newProgress, imageConvertTaskCallbackDto.getResult());
            log.error("imageConvertTaskFinishCallback: task failed, taskId: {}, message {}", taskId,
            taskProcessDto.getResult());
            // 更新任务进度缓存
            redisCache.delCacheMapValue(IMAGE_CONVERT_TASK_PROGRESS_CACHE_KEY, taskId);
            redisCache.setCacheMapValue(IMAGE_CONVERT_TASK_FAILED_CACHE_KEY, taskId, taskProcessDto);
        }
        else if(newStatus == 2) {
            taskProcessDto.updateResult(newStatus, newProgress, imageConvertTaskCallbackDto.getResult());

            Session session = sessionRepository.findByImageId(imageConvertTaskCallbackDto.getImageId());
            Image image = imageRepository.findById(imageConvertTaskCallbackDto.getImageId()).get();
            
            // 根据session的状态执行状态转移
            sessionStatusTransferHandler(session, 1);
            sessionRepository.save(session);
            //同步image的状态
            image.setStatus(session.getStatus());
            imageRepository.save(image);

            // 更新任务进度缓存
            redisCache.delCacheMapValue(IMAGE_CONVERT_TASK_PROGRESS_CACHE_KEY, taskId);
            redisCache.setCacheMapValue(IMAGE_CONVERT_TASK_SUCCESS_CACHE_KEY, taskId, taskProcessDto);
        }

        // 主动推送任务进度
        sendTaskProgress(taskId, taskProcessDto);
        
    }

    public void llmInferenceTaskFinishCallback(@Valid LLMInferenceTaskCallbackDto llmInferenceTaskCallbackDto) {
        String taskId = llmInferenceTaskCallbackDto.getTaskId();
        TaskProcessDto taskProcessDto = redisCache.<TaskProcessDto>getCacheMapValue(LLM_INFERENCE_TASK_PROGRESS_CACHE_KEY, taskId,
                TaskProcessDto.class);

        Integer newStatus = llmInferenceTaskCallbackDto.getStatus();
        Float newProgress = llmInferenceTaskCallbackDto.getProgress();
        
        if(newStatus == 0 || newStatus == 1){
            taskProcessDto.updateProgress(newStatus, newProgress);
            // 保存任务进度缓存
            redisCache.setCacheMapValue(LLM_INFERENCE_TASK_PROGRESS_CACHE_KEY, taskId, taskProcessDto);
        }
        else if(newStatus == 3){
            taskProcessDto.updateResult(newStatus, newProgress, llmInferenceTaskCallbackDto.getResult());
            log.error("llmInferenceTaskFinishCallback: task failed, taskId: {}, message {}", taskId, taskProcessDto.getResult());
            // 更新任务进度缓存
            redisCache.delCacheMapValue(LLM_INFERENCE_TASK_PROGRESS_CACHE_KEY, taskId);
            redisCache.setCacheMapValue(LLM_INFERENCE_TASK_FAILED_CACHE_KEY, taskId, taskProcessDto);
        }
        else if(newStatus == 2){
            taskProcessDto.updateResult(newStatus, newProgress, llmInferenceTaskCallbackDto.getResult());
            System.out.println("保存结果开始");
            Session session = sessionRepository.findByImageId(llmInferenceTaskCallbackDto.getImageId());
            
            QAPair qaPair = qaPairService.findQAPair(null, session.getSessionId(),
                llmInferenceTaskCallbackDto.getLlmTaskTypeId());

            qaPair.setAnswer(taskProcessDto.getResult());
            qaPair.setAnswerTime(new Timestamp(System.currentTimeMillis()));
            qaPairRepository.save(qaPair);

            Map<Integer, QAPair> qaPairPreInferenceTaskMap = session.getQaPairPreInferenceTaskMap();
            qaPairPreInferenceTaskMap.put(llmInferenceTaskCallbackDto.getLlmTaskTypeId(), qaPair);
            
            // 如果所有预处理任务都执行完毕
            if (qaPairPreInferenceTaskMap.size() == llmTaskTypeRepository.countByIsPreProcessTask(true)) {
                // 根据session的状态执行状态转移
                sessionStatusTransferHandler(session, 2);
                //同步image的状态
                Image image = imageRepository.findById(llmInferenceTaskCallbackDto.getImageId()).get();
                image.setStatus(session.getStatus());
                imageRepository.save(image);
            }
            sessionRepository.save(session);

            // 更新任务进度缓存
            redisCache.delCacheMapValue(LLM_INFERENCE_TASK_PROGRESS_CACHE_KEY, taskId);
            redisCache.setCacheMapValue(LLM_INFERENCE_TASK_SUCCESS_CACHE_KEY, taskId, taskProcessDto);
            System.out.println("保存结果结束");
        }
        // 主动推送任务进度
        sendTaskProgress(taskId, taskProcessDto);
        System.out.println("推送任务进度结束");
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


    public void sendTaskProgress(String taskId, TaskProcessDto taskProcessDto) {
        // 使用SimpMessagingTemplate将消息发送到指定的主题
        messagingTemplate.convertAndSend(TASK_PROGRESS_TOPIC + taskId, taskProcessDto);
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

    public String liveQA(@Valid LiveQARequest liveQaRequest) {
        // 查询图片
        Image image = imageRepository.findById(liveQaRequest.getImageId())
                .orElseThrow(() -> new CustomException(CustomError.IMAGE_ID_NOT_FOUND));

        // 查询session
        Session session = sessionRepository.findByImageId(liveQaRequest.getImageId());

        QAPair qaPair = null;
        // 实时问答任务
        if (liveQaRequest.getLlmTaskTypeId() == 1) {
            qaPair = qaPairService.createQAPair(new CreateQAPairDto(session, liveQaRequest.getLlmTaskTypeId(),
                    liveQaRequest.getQuestion(), new Timestamp(System.currentTimeMillis())));

            // 向计算服务发起实时问答请求
            LiveQAToComputationRequest liveQAToComputationRequest = new LiveQAToComputationRequest(
                    liveQaRequest.getQuestion(),
                    image.getImageUrl(),
                    liveQaRequest.getX(),
                    liveQaRequest.getY(),
                    liveQaRequest.getWidth(),
                    liveQaRequest.getHeight());

            WebClient webClient = WebClient.create();
            Mono<LiveQAResponse> response = webClient.post()
                    .uri(computationServerUrl + "/chat")
                    .header("Content-Type", "application/json")
                    .body(Mono.just(liveQAToComputationRequest), LiveQAToComputationRequest.class)
                    .retrieve()
                    .bodyToMono(LiveQAResponse.class);

            LiveQAResponse liveQAResponse = response.block();

            // 这里后面可能要根据模型的回复做一些处理，比如解析xml为前端可用的数据等操作，暂时不用
            qaPair.setAnswer(liveQAResponse.getGpt());
            qaPair.setAnswerTime(new Timestamp(System.currentTimeMillis()));
            qaPairRepository.save(qaPair);
        } else {
            // 预处理任务
            qaPair = qaPairService.findQAPair(null, session.getSessionId(), liveQaRequest.getLlmTaskTypeId());
        }

        session.getQaPairHistoryList().add(qaPair);
        sessionRepository.save(session);
        return qaPair.getAnswer();
    }

}
