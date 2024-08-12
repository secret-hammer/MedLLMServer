package com.vipa.medllm.service.image;

import com.vipa.medllm.dto.middto.task.CreatePathologyImageConvertTaskDto;
import com.vipa.medllm.dto.middto.task.CreatePathologyLLMInferenceTaskDto;
import com.vipa.medllm.dto.request.image.DeleteImageRequest;
import com.vipa.medllm.dto.request.image.UpdateImageInfo;
import com.vipa.medllm.exception.CustomError;
import com.vipa.medllm.exception.CustomException;
import com.vipa.medllm.model.*;
import com.vipa.medllm.repository.ImageGroupRepository;
import com.vipa.medllm.repository.ImageRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.vipa.medllm.dto.request.image.UploadImageRequest;
import com.vipa.medllm.dto.response.SearchResult;
import com.vipa.medllm.repository.ImageTypeRepository;
import com.vipa.medllm.service.session.SessionService;
import com.vipa.medllm.service.task.TaskService;
import com.vipa.medllm.service.user.UserService;
import com.vipa.medllm.util.DirectoryUtil;
import com.vipa.medllm.util.ImageValidator;

import java.io.IOException;
import java.nio.file.*;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ImageService {
    private final UserService userService;
    private final SessionService sessionService;
    private final TaskService taskService;

    private final ImageTypeRepository imageTypeRepository;
    private final ImageValidator imageValidator;
    private final ImageGroupRepository imageGroupRepository;
    private final ImageRepository imageRepository;

    @Value("${medllm.projects.resource.path}")
    private String projectResourcePath;

    @Transactional
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public List<String> uploadImages(@Valid UploadImageRequest uploadImageRequest) {
        List<String> results = new ArrayList<>();
        ImageGroup imageGroup = imageGroupRepository.findById(uploadImageRequest.getImageGroupId())
                .orElseThrow(() -> new CustomException(CustomError.GROUP_NOT_FOUND));
        ImageType imageType = imageTypeRepository.findById(uploadImageRequest.getImageTypeId())
                .orElseThrow(() -> new CustomException(CustomError.IMAGETYPE_NOT_FOUND));

        for (String imageUrl : uploadImageRequest.getImageUrls()) {
            if (imageValidator.isValidImage(imageUrl, imageTypeRepository)) {
                Image image = new Image();
                image.setImageUrl(imageUrl);
                image.setImageName(getImageName(imageUrl));
                image.setImageGroup(imageGroup);
                image.setImageType(imageType);
                imageRepository.save(image);

                // 创建session
                Session session = sessionService.createOneSession(image.getImageId());

                // 创建静态资源服务器图片文件夹
                createImageFolder(image);

                startTasks(image, session);

                results.add("Success!");
            } else {
                results.add("Failed: " + imageUrl + " - Invalid image format");
            }
        }
        return results;
    }

    @Transactional
    public SearchResult<Image> searchImages(Integer imageId, Integer imageGroupId, String imageName, String imageUrl,
            Integer imageTypeId, Integer page, Integer size) {
        Specification<Image> spec = Specification.where(null);

        spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder
                .equal(root.get("imageGroup").get("imageGroupId"), imageGroupId));

        if (imageId != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("imageId"), imageId));
        }

        if (imageTypeId != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder
                    .equal(root.get("imageType").get("imageTypeId"), imageTypeId));
        }

        if (imageName != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("imageName"),
                    "%" + imageName + "%"));
        }

        if (imageUrl != null) {
            spec = spec.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("imageUrl"), "%" + imageUrl + "%"));
        }

        SearchResult<Image> searchResult = new SearchResult<>();
        List<Image> selectedImages = null;

        if (page != null && size != null && page >= 0 && size > 0) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("imageId").ascending());
            Page<Image> imagePage = imageRepository.findAll(spec, pageable);
            searchResult.setPageInfo(page, size, imagePage.getTotalPages(), imagePage.getTotalElements(),
                    imagePage.isFirst(), imagePage.isLast(), imagePage.isEmpty());
            selectedImages = imagePage.getContent();
        } else {
            selectedImages = imageRepository.findAll(spec);
        }

        // 对 selectedImages 进行排序，将精确匹配的放前面，模糊匹配的放后面
        List<Image> sortedImages = selectedImages.stream()
                .sorted((i1, i2) -> {
                    int i1Match = 0;
                    int i2Match = 0;

                    if (imageName != null && !imageName.isEmpty()) {
                        if (i1.getImageName().equals(imageName)) {
                            i1Match += 2;
                        } else if (i1.getImageName().contains(imageName)) {
                            i1Match += 1;
                        }
                        if (i2.getImageName().equals(imageName)) {
                            i2Match += 2;
                        } else if (i2.getImageName().contains(imageName)) {
                            i2Match += 1;
                        }
                    }

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        if (i1.getImageUrl().equals(imageUrl)) {
                            i1Match += 2;
                        } else if (i1.getImageUrl().contains(imageUrl)) {
                            i1Match += 1;
                        }
                        if (i2.getImageUrl().equals(imageUrl)) {
                            i2Match += 2;
                        } else if (i2.getImageUrl().contains(imageUrl)) {
                            i2Match += 1;
                        }
                    }

                    return Integer.compare(i2Match, i1Match); // 按匹配程度降序排列
                }).toList();

        searchResult.setContent(sortedImages);
        return searchResult;
    }

    @Transactional
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void updateImage(List<UpdateImageInfo> updateImageRequest) {
        for (UpdateImageInfo updateImageInfo : updateImageRequest) {
            Image image = imageRepository.findById(updateImageInfo.getImageId())
                    .orElseThrow(() -> new CustomException(CustomError.IMAGE_ID_NOT_FOUND));

            if (updateImageInfo.getNewImageGroupId() > 0) {
                ImageGroup imageGroup = imageGroupRepository.findById(updateImageInfo.getNewImageGroupId())
                        .orElseThrow(() -> new CustomException(CustomError.GROUP_NOT_FOUND));
                image.setImageGroup(imageGroup);
            }
            if (updateImageInfo.getNewImageName() != null && !updateImageInfo.getNewImageName().isEmpty()) {
                image.setImageName(updateImageInfo.getNewImageName());
            }

            // 保存更新后的实体
            imageRepository.save(image);
        }
    }

    @Transactional
    public void deleteImages(@Valid DeleteImageRequest deleteImageRequest) {
        List<Image> images = new ArrayList<>();
        for (Integer imageId : deleteImageRequest.getImageIds()) {
            // 如果找不到不要报错
            Image image = imageRepository.findById(imageId).orElse(null);
            if (image == null)
                throw new CustomException(CustomError.IMAGE_ID_NOT_FOUND);
            images.add(image);
        }
        for (Image image : images) {
            deleteImageFolder(image);
            imageRepository.delete(image);
        }
    }

    private void deleteImageFolder(Image image) {
        Project project = image.getImageGroup().getProject();
        String folderPath = String.format(projectResourcePath + "/projects/%d/%d", project.getProjectId(),
                image.getImageId());

        Path dir = Paths.get(folderPath);
        if (Files.notExists(dir)) {
            log.error("com.vipa.medllm.service.image.deleteImageFolder: Image folder not found: " + folderPath);
        } else {
            try {
                DirectoryUtil.deleteDirectory(dir);
            } catch (IOException e) {
                log.error(
                        "com.vipa.medllm.service.image.deleteImageFolder: Error deleting image folder: " + folderPath);
            }
        }
    }

    private void createImageFolder(Image image) {
        Project project = image.getImageGroup().getProject();
        String folderPath = String.format(projectResourcePath + "/projects/%d/%d", project.getProjectId(),
                image.getImageId());

        Path dir = Paths.get(folderPath);
        if (Files.exists(dir)) {
            log.error("com.vipa.medllm.service.image.createImageFolder: Image folder already exists: " + folderPath);
        } else {
            try {
                DirectoryUtil.createDirectory(dir);
            } catch (IOException e) {
                log.error(
                        "com.vipa.medllm.service.image.createImageFolder: Error creating image folder: " + folderPath);
            }
        }
    }

    private String getImageName(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
    }

    private void startTasks(Image image, Session session) {
        // 发布病理图转换任务
        CreatePathologyImageConvertTaskDto createPathologyImageConvertTaskDto = new CreatePathologyImageConvertTaskDto(
                image.getImageGroup().getProject(), image);

        taskService.submitPathologyImageConvertTask(createPathologyImageConvertTaskDto);

        // 发布所有大模型推理任务
        List<LLMTaskType> llmTaskTypeList = taskService.searchLLMTaskType(null, true, null, null, null);

        for (LLMTaskType llmTaskType : llmTaskTypeList) {
            CreatePathologyLLMInferenceTaskDto createPathologyLLMInferenceTaskDto = new CreatePathologyLLMInferenceTaskDto(
                    session, image, llmTaskType);
            taskService.submitPathologyLLMInferenceTask(createPathologyLLMInferenceTaskDto);
        }
    }
}
