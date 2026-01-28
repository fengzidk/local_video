package com.example.localvideo.service;

import org.bytedeco.ffmpeg.global.avcodec;
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

    public File createClip(String relativePath, double start, double end) throws IOException {
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
            long endUs = (long) (end * 1_000_000L);
            grabber.setTimestamp(startUs);

            int w = Math.max(1, grabber.getImageWidth());
            int h = Math.max(1, grabber.getImageHeight());
            int ch = Math.max(0, grabber.getAudioChannels());
            double fps = grabber.getVideoFrameRate() > 0 ? grabber.getVideoFrameRate() : 25.0;

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(out, w, h, ch)) {
                recorder.setFormat("mp4");
                recorder.setFrameRate(fps);
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                if (ch > 0) {
                    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                }
                // Use default CRF for quality. If this fails, consider removing or checking codec support.
                recorder.setVideoOption("crf", "23");
                recorder.setVideoOption("preset", "veryfast");
                recorder.setOption("movflags", "+faststart");
                recorder.start();

                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    long ts = grabber.getTimestamp();
                    if (ts >= endUs) break;
                    recorder.record(frame);
                }
                recorder.stop();
            }
            grabber.stop();
        } catch (Exception e) {
            throw new IOException("clip failed: " + e.getMessage(), e);
        }
        if (!out.exists() || out.length() == 0) throw new IOException("empty output");
        return out;
    }
}
