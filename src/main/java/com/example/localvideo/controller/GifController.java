package com.example.localvideo.controller;

import com.example.localvideo.service.GifService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GifController {
    private final GifService gifService;

    public GifController(GifService gifService) {
        this.gifService = gifService;
    }

    @PostMapping("/gif")
    public ResponseEntity<Map<String, Object>> gif(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String path = (String) body.get("path");
            double start = number(body.get("start"));
            double duration = number(body.get("duration"));
            int width = body.get("width") == null ? 480 : ((Number) body.get("width")).intValue();
            File out = gifService.createGif(path, start, duration, width);
            resp.put("url", "/output/gifs/" + out.getName());
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
