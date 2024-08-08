package com.vipa.medllm.service.task;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.vipa.medllm.dto.middto.task.ImageConvertTaskCallbackDto;
import com.vipa.medllm.dto.middto.task.LLMInferenceTaskCallbackDto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CallbackListener {
    private final TaskService taskService;

    @RabbitListener(queues = "image_convert_task_finish_callback_queue")
    public void imageConvertTaskFinishCallback(@Valid ImageConvertTaskCallbackDto imageConvertTaskCallbackDto) {
        taskService.imageConvertTaskFinishCallback(imageConvertTaskCallbackDto);
    }

    @RabbitListener(queues = "llm_inference_task_finish_callback_queue")
    public void llmInferenceTaskFinishCallback(@Valid LLMInferenceTaskCallbackDto llmInferenceTaskCallbackDto) {
        taskService.llmInferenceTaskFinishCallback(llmInferenceTaskCallbackDto);
    }
}
