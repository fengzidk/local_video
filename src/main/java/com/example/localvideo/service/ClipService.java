package com.example.localvideo.service;

import org.bytedeco.ffmpeg.global.avcodec;
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
public class ClipService {
    @Value("${app.videoBaseDir}")
    private String videoBaseDir;

    @Value("${app.clipCacheDir}")
    private String clipCacheDir;

    /**
     * @param width 目标宽度，0 表示保持原始分辨率
     */
    public File createClip(String relativePath, double start, double end, int width) throws IOException {
        if (end <= start) throw new IOException("end must be > start");
        File base = new File(videoBaseDir);
        File input = new File(base, relativePath);
        if (!input.exists()) throw new IOException("input not found: " + input.getAbsolutePath());
        File outDir = new File(clipCacheDir);
        if (!outDir.exists()) outDir.mkdirs();
        String name = UUID.randomUUID().toString().replace("-", "") + ".mp4";
        File out = new File(outDir, name);

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input)) {
            grabber.start();
            long startUs = (long) (start * 1_000_000L);
            long endUs   = (long) (end   * 1_000_000L);

            int srcW = Math.max(1, grabber.getImageWidth());
            int srcH = Math.max(1, grabber.getImageHeight());
            int ch   = Math.max(0, grabber.getAudioChannels());
            double fps = grabber.getVideoFrameRate() > 0 ? grabber.getVideoFrameRate() : 25.0;

            // 计算目标尺寸：0 或超过原始宽度时保持原始
            int targetW, targetH;
            if (width <= 0 || width >= srcW) {
                targetW = srcW;
                targetH = srcH;
            } else {
                targetW = width;
                targetH = (int) Math.round((double) srcH * targetW / srcW);
                // H.264 要求宽高为偶数
                if (targetW % 2 != 0) targetW--;
                if (targetH % 2 != 0) targetH--;
                targetW = Math.max(2, targetW);
                targetH = Math.max(2, targetH);
            }

            boolean needScale = (targetW != srcW || targetH != srcH);
            FFmpegFrameFilter filter = null;
            if (needScale) {
                String expr = "scale=" + targetW + ":" + targetH + ":flags=lanczos";
                filter = new FFmpegFrameFilter(expr, srcW, srcH);
                filter.setPixelFormat(grabber.getPixelFormat());
                filter.start();
            }

            grabber.setTimestamp(startUs);

            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(out, targetW, targetH, ch);
            recorder.setFormat("mp4");
            recorder.setFrameRate(fps);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            if (ch > 0) recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorder.setVideoOption("crf", "18");
            recorder.setVideoOption("preset", "veryfast");
            recorder.setOption("movflags", "+faststart");
            recorder.start();

            try {
                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    long ts = grabber.getTimestamp();
                    if (ts >= endUs) break;
                    if (needScale && frame.image != null) {
                        filter.push(frame);
                        Frame scaled;
                        while ((scaled = filter.pull()) != null) {
                            recorder.record(scaled);
                        }
                    } else {
                        recorder.record(frame);
                    }
                }
                if (needScale && filter != null) {
                    filter.push(null);
                    Frame scaled;
                    while ((scaled = filter.pull()) != null) {
                        recorder.record(scaled);
                    }
                }
            } finally {
                recorder.stop();
                recorder.release();
                if (filter != null) { filter.stop(); filter.release(); }
                grabber.stop();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("clip failed: " + e.getMessage(), e);
        }
        if (!out.exists() || out.length() == 0) throw new IOException("empty output");
        return out;
    }
}
