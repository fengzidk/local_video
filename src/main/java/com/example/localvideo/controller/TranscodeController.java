package com.example.localvideo.controller;

import com.example.localvideo.service.TranscodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TranscodeController {
    private final TranscodeService transcodeService;

    public TranscodeController(TranscodeService transcodeService) {
        this.transcodeService = transcodeService;
    }

    @PostMapping("/transcode")
    public ResponseEntity<Map<String, Object>> transcode(@RequestBody Map<String, Object> body) throws IOException {
        String path = (String) body.get("path");
        File out = transcodeService.getPlayableMp4(path);
        Map<String, Object> resp = new HashMap<>();
        resp.put("url", "/output/streams/" + out.getName());
        resp.put("name", out.getName());
        return ResponseEntity.ok(resp);
    }
}
