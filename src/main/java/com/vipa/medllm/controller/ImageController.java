package com.vipa.medllm.controller;

import com.vipa.medllm.dto.request.image.DeleteImageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vipa.medllm.dto.request.image.UploadImageRequest;
import com.vipa.medllm.dto.response.ResponseResult;
import com.vipa.medllm.service.image.ImageService;

import lombok.AllArgsConstructor;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/image")
public class ImageController {
    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<ResponseResult<List<String>>> uploadImages(@RequestBody UploadImageRequest uploadImageRequest) {

        List<String> results = imageService.uploadImages(uploadImageRequest);
        ResponseResult<List<String>> response = new ResponseResult<>(200, "Image upload process completed", results);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    public ResponseEntity<ResponseResult<List<String>>> deleteImages(@RequestBody DeleteImageRequest deleteImageRequest) {

        List<String> results = imageService.deleteImages(deleteImageRequest);
        ResponseResult<List<String>> response = new ResponseResult<>(200, "Image delete process completed", results);

        return ResponseEntity.ok(response);
    }
}
