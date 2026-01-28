package com.example.localvideo.controller;

import com.example.localvideo.service.ClipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClipController {
    private final ClipService clipService;

    public ClipController(ClipService clipService) {
        this.clipService = clipService;
    }

    @PostMapping("/clip")
    public ResponseEntity<Map<String, Object>> clip(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String path = (String) body.get("path");
            double start = number(body.get("start"));
            double end = number(body.get("end"));
            File out = clipService.createClip(path, start, end);
            resp.put("url", "/output/clips/" + out.getName());
            resp.put("name", out.getName());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("error", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    private double number(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return Double.parseDouble(String.valueOf(o));
    }
}
