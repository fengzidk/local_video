package com.example.localvideo.service;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class ThumbnailService {

    @Value("${app.videoBaseDir}")
    private String videoBaseDir;

    @Value("${app.thumbnailCacheDir}")
    private String thumbnailCacheDir;

    private final Java2DFrameConverter converter = new Java2DFrameConverter();

    public File getOrCreateThumbnail(String relativePath) throws IOException {
        File base = new File(videoBaseDir);
        File video = new File(base, relativePath);
        if (!video.exists()) {
            throw new IOException("Video not found: " + video.getAbsolutePath());
        }
        File cacheDir = new File(thumbnailCacheDir);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        String key = sha1(relativePath);
        File thumb = new File(cacheDir, key + ".jpg");
        if (thumb.exists() && thumb.length() > 0) {
            return thumb;
        }
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(video)) {
            grabber.start();
            long duration = grabber.getLengthInTime(); // microseconds
            // 候选抓帧时间点：10%、25%、1秒、0秒
            long[] candidates;
            if (duration > 0) {
                long t10 = duration / 10;
                long t25 = duration / 4;
                long t1s = 1_000_000L;
                candidates = new long[]{ t10, t25, t1s, 0 };
            } else {
                candidates = new long[]{ 1_000_000L, 0 };
            }
            BufferedImage img = null;
            for (long ts : candidates) {
                if (ts < 0) ts = 0;
                if (duration > 0 && ts >= duration) ts = Math.max(0, duration - 1_000_000L);
                try {
                    grabber.setTimestamp(ts);
                    org.bytedeco.javacv.Frame frame = grabber.grabImage();
                    if (frame != null) {
                        img = converter.getBufferedImage(frame, 1.0);
                        if (img != null) break;
                    }
                } catch (Exception ignored) {}
            }
            if (img == null) {
                // 最后尝试顺序读取前几帧
                grabber.setTimestamp(0);
                for (int i = 0; i < 30; i++) {
                    org.bytedeco.javacv.Frame frame = grabber.grabImage();
                    if (frame == null) break;
                    img = converter.getBufferedImage(frame, 1.0);
                    if (img != null) break;
                }
            }
            grabber.stop();
            if (img == null) throw new IOException("failed to grab any frame from: " + relativePath);
            ImageIO.write(img, "jpg", thumb);
            return thumb;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("thumbnail failed: " + e.getMessage(), e);
        }
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
