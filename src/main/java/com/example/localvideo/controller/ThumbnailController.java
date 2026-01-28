package com.example.localvideo.controller;

import com.example.localvideo.service.ThumbnailService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api")
public class ThumbnailController {

    private final ThumbnailService thumbnailService;

    public ThumbnailController(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    @GetMapping(value = "/thumbnail", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> thumbnail(@RequestParam("path") String relativePath) throws IOException {
        File thumb = thumbnailService.getOrCreateThumbnail(relativePath);
        byte[] bytes = Files.readAllBytes(thumb.toPath());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes);
    }
}
