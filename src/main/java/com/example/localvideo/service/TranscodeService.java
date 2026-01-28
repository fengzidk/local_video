package com.example.localvideo.service;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class TranscodeService {
    @Value("${app.videoBaseDir}")
    private String videoBaseDir;

    @Value("${app.streamCacheDir}")
    private String streamCacheDir;

    public File getPlayableMp4(String relativePath) throws IOException {
        File base = new File(videoBaseDir);
        File input = new File(base, relativePath);
        if (!input.exists()) throw new IOException("input not found");
        File outDir = new File(streamCacheDir);
        if (!outDir.exists()) outDir.mkdirs();
        String key = sha1(relativePath) + ".mp4";
        File out = new File(outDir, key);
        if (out.exists() && out.length() > 0) {
            return out;
        }
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input)) {
            grabber.start();
            int w = Math.max(1, grabber.getImageWidth());
            int h = Math.max(1, grabber.getImageHeight());
            int ch = Math.max(0, grabber.getAudioChannels());
            double fps = grabber.getVideoFrameRate() > 0 ? grabber.getVideoFrameRate() : 25.0;
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(out, w, h, ch)) {
                recorder.setFormat("mp4");
                recorder.setFrameRate(fps);
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setVideoOption("crf", "23");
                recorder.setVideoOption("preset", "veryfast");
                recorder.setOption("movflags", "+faststart");
                recorder.start();
                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    recorder.record(frame);
                }
                recorder.stop();
            }
            grabber.stop();
        } catch (Exception e) {
            throw new IOException("transcode failed: " + e.getMessage(), e);
        }
        if (!out.exists() || out.length() == 0) throw new IOException("empty transcode output");
        return out;
    }

    private String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
