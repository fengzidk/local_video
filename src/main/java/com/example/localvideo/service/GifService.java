package com.example.localvideo.service;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
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
        if (!input.exists()) throw new IOException("input not found: " + input.getAbsolutePath());
        File outDir = new File(gifCacheDir);
        if (!outDir.exists()) outDir.mkdirs();
        File out = new File(outDir, UUID.randomUUID().toString().replace("-", "") + ".gif");
        double fps = 15.0;
        long startUs = (long)(start * 1_000_000L);
        long endUs   = (long)((start + duration) * 1_000_000L);
        
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input)) {
            grabber.start();
            int srcW = Math.max(1, grabber.getImageWidth());
            int srcH = Math.max(1, grabber.getImageHeight());
            int targetW = (width > 0 ? Math.min(width, srcW) : srcW);
            // GIF 要求宽高均为偶数
            if (targetW % 2 != 0) targetW--;
            int targetH = (int) Math.round((double) srcH * targetW / srcW);
            if (targetH % 2 != 0) targetH--;
            targetW = Math.max(2, targetW);
            targetH = Math.max(2, targetH);
        
            // 简单 scale+fps 滤镜，兼容性最好
            String filterExpr = "fps=" + fps + ",scale=" + targetW + ":" + targetH + ":flags=lanczos";
            FFmpegFrameFilter filter = new FFmpegFrameFilter(filterExpr, srcW, srcH);
            filter.setPixelFormat(grabber.getPixelFormat());
            filter.start();
        
            grabber.setTimestamp(startUs);
        
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(out.getAbsolutePath(), targetW, targetH, 0);
            recorder.setFormat("gif");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_GIF);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_RGB8);
            recorder.setFrameRate(fps);
            recorder.setVideoOption("loop", "0");
            recorder.start();

            try {
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
                // flush filter
                filter.push(null);
                Frame outF;
                while ((outF = filter.pull()) != null) {
                    recorder.record(outF);
                }
            } finally {
                recorder.stop();
                recorder.release();
                filter.stop();
                filter.release();
                grabber.stop();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("gif failed: " + e.getMessage(), e);
        }
        if (!out.exists() || out.length() == 0) throw new IOException("empty gif output");
        return out;
    }
}
