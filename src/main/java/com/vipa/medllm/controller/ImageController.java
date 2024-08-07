package com.vipa.medllm.controller;

import com.vipa.medllm.dto.request.image.DeleteImageRequest;
import com.vipa.medllm.dto.request.image.UpdateImageInfo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vipa.medllm.dto.request.image.UploadImageRequest;
import com.vipa.medllm.dto.response.ResponseResult;
import com.vipa.medllm.model.Image;
import com.vipa.medllm.service.image.ImageService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/image")
public class ImageController {
    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<ResponseResult<List<String>>> uploadImages(
            @Valid @RequestBody UploadImageRequest uploadImageRequest) {

        List<String> results = imageService.uploadImages(uploadImageRequest);
        ResponseResult<List<String>> response = new ResponseResult<>(200, "Image upload process completed", results);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ResponseResult<Object>> updateImages(
            @Valid @RequestBody List<UpdateImageInfo> updateImageRequest) {

        imageService.updateImage(updateImageRequest);
        ResponseResult<Object> response = new ResponseResult<>(200, "Images updated completed");

        return ResponseEntity.ok(response);
    }

    @GetMapping("search")
    public ResponseEntity<ResponseResult<List<Image>>> searchImages(
            @RequestParam(required = true) Integer projectId, @RequestParam(required = false) Integer groupId) {

        List<Image> images = imageService.searchImages(projectId, groupId);

        ResponseResult<List<Image>> response = new ResponseResult<>(200, "Image search successfully", images);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    public ResponseEntity<ResponseResult<List<String>>> deleteImages(
            @Valid @RequestBody DeleteImageRequest deleteImageRequest) {

        imageService.deleteImages(deleteImageRequest);
        ResponseResult<List<String>> response = new ResponseResult<>(200, "Image delete process completed");

        return ResponseEntity.ok(response);
    }
}
