package com.vipa.medllm.service.image;

import com.vipa.medllm.dto.request.image.DeleteImageRequest;
import com.vipa.medllm.dto.request.image.UpdateImageInfo;
import com.vipa.medllm.dto.request.image.SearchImageRequest;
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

import com.vipa.medllm.dto.request.image.UploadImageRequest;
import com.vipa.medllm.repository.ImageTypeRepository;
import com.vipa.medllm.service.session.SessionService;
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
        User user = userService.getCurrentUser();
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

                //创建session
                sessionService.createSession(List.of(image.getImageId()));

                // 创建静态资源服务器图片文件夹
                createImageFolder(image);

                // startTasks(imageUrl); //todo: 待添加的接口
                
                results.add("Success!");
            } else {
                results.add("Failed: " + imageUrl + " - Invalid image format");
            }
        }
        return results;
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

    @Transactional
    public List<Image> searchImages(SearchImageRequest searchImageRequest) {
        Specification<Image> spec = Specification.where(null);

        if(searchImageRequest.getImageGroupId() != null){
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("imageGroup").get("imageGroupId"), searchImageRequest.getImageGroupId()));
        }

        if(searchImageRequest.getImageId() != null){
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("imageId"), searchImageRequest.getImageId()));
        }

        if(searchImageRequest.getImageTypeId()!=null){
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("imageType").get("imageTypeId"), searchImageRequest.getImageTypeId()));
        }

        if(searchImageRequest.getImageName()!=null){
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("imageName"), "%" + searchImageRequest.getImageName() + "%"));
        }

        if(searchImageRequest.getImageUrl() != null){
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("imageUrl"), "%" + searchImageRequest.getImageUrl() + "%"));
        }

        List<Image> selectedImages = imageRepository.findAll(spec);

        // 对 selectedImages 进行排序，将精确匹配的放前面，模糊匹配的放后面
        List<Image> sortedImages = selectedImages.stream()
        .sorted((i1, i2) -> {
            int i1Match = 0;
            int i2Match = 0;

            if (searchImageRequest.getImageName() != null && !searchImageRequest.getImageName().isEmpty()) {
                if (i1.getImageName().equals(searchImageRequest.getImageName())) {
                    i1Match += 2;
                } else if (i1.getImageName().contains(searchImageRequest.getImageName())) {
                    i1Match += 1;
                }
                if (i2.getImageName().equals(searchImageRequest.getImageName())) {
                    i2Match += 2;
                } else if (i2.getImageName().contains(searchImageRequest.getImageName())) {
                    i2Match += 1;
                }
            }

            if (searchImageRequest.getImageUrl() != null && !searchImageRequest.getImageUrl().isEmpty()) {
                if (i1.getImageUrl().equals(searchImageRequest.getImageUrl())) {
                    i1Match += 2;
                } else if (i1.getImageUrl().contains(searchImageRequest.getImageUrl())) {
                    i1Match += 1;
                }
                if (i2.getImageUrl().equals(searchImageRequest.getImageUrl())) {
                    i2Match += 2;
                } else if (i2.getImageUrl().contains(searchImageRequest.getImageUrl())) {
                    i2Match += 1;
                }
            }

            return Integer.compare(i2Match, i1Match); // 按匹配程度降序排列
        }).toList();


        return sortedImages;
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

    public void deleteImageFolder(Image image) {
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

    private String getImageName(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
    }
}
