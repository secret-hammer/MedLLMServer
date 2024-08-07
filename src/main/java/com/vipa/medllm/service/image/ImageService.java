package com.vipa.medllm.service.image;

import com.vipa.medllm.dto.request.image.DeleteImageRequest;
import com.vipa.medllm.exception.CustomError;
import com.vipa.medllm.exception.CustomException;
import com.vipa.medllm.model.Image;
import com.vipa.medllm.model.ImageGroup;
import com.vipa.medllm.model.ImageType;
import com.vipa.medllm.repository.ImageGroupRepository;
import com.vipa.medllm.repository.ImageRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.vipa.medllm.dto.request.image.UploadImageRequest;
import com.vipa.medllm.repository.ImageTypeRepository;
import com.vipa.medllm.util.ImageValidator;

import java.io.File;

import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ImageService {
    private final ImageTypeRepository imageTypeRepository;
    private final ImageValidator imageValidator;
    private final ImageGroupRepository imageGroupRepository;
    private final ImageRepository imageRepository;

    @Transactional
    @Retryable(retryFor = {SQLException.class, ObjectOptimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public List<String> uploadImages(UploadImageRequest uploadImageRequest) {
        List<String> results = new ArrayList<>();
        ImageGroup imageGroup = imageGroupRepository.findById(uploadImageRequest.getImageGroupId()).orElseThrow(() -> new RuntimeException("Image group not found"));
        ImageType imageType = imageTypeRepository.findById(uploadImageRequest.getImageTypeId()).orElseThrow(() -> new RuntimeException("Image type not found"));

        for (String imageUrl : uploadImageRequest.getImageUrls()) {
            if (imageValidator.isValidImage(imageUrl, imageTypeRepository)) {
                Image image = new Image();
                image.setImageUrl(imageUrl);
                image.setImageName(getImageName(imageUrl));
                image.setImageGroup(imageGroup);
                image.setImageType(imageType);
                imageRepository.save(image);
//                startTasks(imageUrl); //todo: 待添加的接口
                createImageFolder(imageGroup, image);
                results.add("Success!");
            } else {
                results.add("Failed: " + imageUrl + " - Invalid image format");
            }
        }
        return results;
    }

    private void createImageFolder(ImageGroup imageGroup, Image image) {
        String projectName = imageGroup.getProject().getProjectName();
        String folderPath = String.format("./medllm_dev/projects/%d-%s/%d-%s", imageGroup.getProject().getProjectId(), projectName, image.getImageId(), image.getImageName());

        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    // @Transactional
    // public void deleteImages(DeleteImageRequest deleteImageRequest) {
    //     List<Image> images = new ArrayList<>(List.of());
    //     for (Integer imageId : deleteImageRequest.getImageIds()) {
    //         // 如果找不到不要报错
    //         Image image = imageRepository.findById(imageId).orElse(null);
    //         if (image == null)  
    //             throw new CustomException(CustomError.IMAGE_ID_NOT_FOUND);
    //         images.add(image);
    //     }
    //     for (Image image : images) {
    //         imageRepository.delete(image);
    //         if (!deleteImageFolder(image)) //直接记录日志：log.error
    //             log.error("Cannot find target dictory with this URL: " + image.getImageUrl());
    //     }
    // }

    // public static Boolean deleteImageFolder(Image image) {
    //     String projectName = image.getImageGroup().getProject().getProjectName();
    //     String folderPath = String.format("./medllm_dev/projects/%d-%s/%d-%s", image.getImageGroup().getProject().getProjectId(), projectName, image.getImageId(), image.getImageName());

    //     File folder = new File(folderPath);
    //     return deleteDirectory(folder);
    // }

    // public static Boolean deleteDirectory(File directory) {
    //     if (directory.isDirectory()) {
    //         File[] files = directory.listFiles();
    //         if (files != null) {
    //             for (File file : files) {
    //                 deleteDirectory(file);
    //             }
    //         }
    //     }
    //     return directory.delete();
    // }

    // private String getImageName(String imageUrl) {
    //     return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
    // }
}
