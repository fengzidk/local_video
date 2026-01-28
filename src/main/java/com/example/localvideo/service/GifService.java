package com.example.localvideo.service;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.Frame;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class GifService {
    @Value("${app.videoBaseDir}")
    private String videoBaseDir;

    @Value("${app.gifCacheDir}")
    private String gifCacheDir;

    public File createGif(String relativePath, double start, double duration, int width) throws IOException {
        if (duration <= 0) throw new IOException("duration must be > 0");
        File base = new File(videoBaseDir);
        File input = new File(base, relativePath);
        if (!input.exists()) throw new IOException("input not found");
        File outDir = new File(gifCacheDir);
        if (!outDir.exists()) outDir.mkdirs();
        File out = new File(outDir, UUID.randomUUID().toString().replace("-", "") + ".gif");
        double fps = 12.0;
        long startUs = (long)(start * 1_000_000L);
        long endUs = (long)((start + duration) * 1_000_000L);

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input)) {
            grabber.start();
            int srcW = Math.max(1, grabber.getImageWidth());
            int srcH = Math.max(1, grabber.getImageHeight());
            int targetW = Math.max(1, width);
            int targetH = (int)Math.round((double)srcH * targetW / srcW);
            // Ensure even dimensions to avoid some codec issues, though less critical for GIF
            if (targetH % 2 != 0) targetH--;

            String filterExpr = "scale=" + targetW + ":" + targetH + ":flags=lanczos,fps=" + fps;
            FFmpegFrameFilter filter = new FFmpegFrameFilter(filterExpr, srcW, srcH);
            filter.start();
            grabber.setTimestamp(startUs);
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(out, targetW, targetH, 0)) {
                recorder.setFormat("gif");
                recorder.setFrameRate(fps);
                recorder.start();
                Frame f;
                while ((f = grabber.grabImage()) != null) {
                    long ts = grabber.getTimestamp();
                    if (ts >= endUs) break;
                    filter.push(f);
                    Frame outF;
                    while ((outF = filter.pull()) != null) {
                        recorder.record(outF);
                    }
                }
                recorder.stop();
            }
            filter.stop();
            grabber.stop();
        } catch (Exception e) {
            throw new IOException("gif failed: " + e.getMessage(), e);
        }
        if (!out.exists() || out.length() == 0) throw new IOException("empty gif output");
        return out;
    }
}
