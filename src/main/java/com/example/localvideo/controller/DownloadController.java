package com.example.localvideo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/api")
public class DownloadController {

    @Value("${app.clipCacheDir}")
    private String clipCacheDir;

    @Value("${app.gifCacheDir}")
    private String gifCacheDir;

    @GetMapping("/download/clip")
    public ResponseEntity<FileSystemResource> downloadClip(@RequestParam("name") String name) {
        File file = safeFile(clipCacheDir, name);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource res = new FileSystemResource(file);
        String encoded = urlEncode(file.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"; filename*=UTF-8''" + encoded)
                .header("Cache-Control", "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(file.length())
                .contentType(MediaType.valueOf("video/mp4"))
                .body(res);
    }

    @GetMapping("/download/gif")
    public ResponseEntity<FileSystemResource> downloadGif(@RequestParam("name") String name) {
        File file = safeFile(gifCacheDir, name);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource res = new FileSystemResource(file);
        String encoded = urlEncode(file.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"; filename*=UTF-8''" + encoded)
                .header("Cache-Control", "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(file.length())
                .contentType(MediaType.IMAGE_GIF)
                .body(res);
    }

    private File safeFile(String baseDir, String name) {
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "");
        return new File(baseDir, safe);
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }
}
