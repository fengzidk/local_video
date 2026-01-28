package com.example.localvideo.controller;

import com.example.localvideo.model.PageResponse;
import com.example.localvideo.model.VideoFile;
import com.example.localvideo.service.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VideoController {

    private final VideoService videoService;

    @Value("${app.videoBaseDir}")
    private String videoBaseDir;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/videos")
    public PageResponse<VideoFile> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        List<VideoFile> items = videoService.getPage(page, pageSize);
        long total = videoService.totalCount();
        return new PageResponse<>(page, pageSize, total, items);
    }

    @PostMapping("/rescan")
    public ResponseEntity<Void> rescan() {
        videoService.rescan();
        return ResponseEntity.noContent().build();
    }
}
