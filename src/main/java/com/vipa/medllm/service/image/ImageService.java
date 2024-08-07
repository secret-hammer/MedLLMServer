package com.vipa.medllm.service.image;

import com.vipa.medllm.dto.request.image.DeleteImageRequest;
import com.vipa.medllm.exception.CustomError;
import com.vipa.medllm.exception.CustomException;
import com.vipa.medllm.model.Image;
import com.vipa.medllm.model.ImageGroup;
import com.vipa.medllm.model.ImageType;
import com.vipa.medllm.model.Project;
import com.vipa.medllm.repository.ImageGroupRepository;
import com.vipa.medllm.repository.ImageRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.vipa.medllm.dto.request.image.UploadImageRequest;
import com.vipa.medllm.repository.ImageTypeRepository;
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
                .orElseThrow(() -> new RuntimeException("Image group not found"));
        ImageType imageType = imageTypeRepository.findById(uploadImageRequest.getImageTypeId())
                .orElseThrow(() -> new RuntimeException("Image type not found"));

        for (String imageUrl : uploadImageRequest.getImageUrls()) {
            if (imageValidator.isValidImage(imageUrl, imageTypeRepository)) {
                Image image = new Image();
                image.setImageUrl(imageUrl);
                image.setImageName(getImageName(imageUrl));
                image.setImageGroup(imageGroup);
                image.setImageType(imageType);
                imageRepository.save(image);
                // startTasks(imageUrl); //todo: 待添加的接口
                createImageFolder(image);
                results.add("Success!");
            } else {
                results.add("Failed: " + imageUrl + " - Invalid image format");
            }
        }
        return results;
    }

    private void createImageFolder(Image image) {
        Project project = image.getImageGroup().getProject();
        String folderPath = String.format(projectResourcePath + "/projects/%d-%s/%d-%s", project.getProjectId(),
                project.getProjectName(), image.getImageId(), image.getImageName());

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

    public void deleteImageFolder(Image image) {
        Project project = image.getImageGroup().getProject();
        String folderPath = String.format(projectResourcePath + "/projects/%d-%s/%d-%s", project.getProjectId(),
                project.getProjectName(), image.getImageId(), image.getImageName());

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

    private String getImageName(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
    }
}
