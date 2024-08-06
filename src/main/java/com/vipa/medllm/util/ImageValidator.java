package com.vipa.medllm.util;

import java.util.List;
import java.util.stream.Stream;

import com.vipa.medllm.repository.ImageTypeRepository;
import org.springframework.stereotype.Component;

@Component
public class ImageValidator {

    public boolean isValidImage(String imageUrl, ImageTypeRepository imageTypeRepository) {
        String fileExtension = getFileExtension(imageUrl);
//        List<String> validExtensions = imageTypeRepository.findAllExtensions().stream()
//                .flatMap(ext -> Stream.of(ext.split(","))).toList();
        // 指定为【"tif", "mrxs"】
        List<String> validExtensions = List.of("tif", "mrxs");
        return validExtensions.contains(fileExtension);
    }

    private String getFileExtension(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf('.') + 1);
    }
}
