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
import java.util.Random;

@Service
public class ThumbnailService {

    @Value("${app.videoBaseDir}")
    private String videoBaseDir;

    @Value("${app.thumbnailCacheDir}")
    private String thumbnailCacheDir;

    private final Random random = new Random();

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
        int sec = 1 + random.nextInt(30);
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(video)) {
            grabber.start();
            grabber.setTimestamp(sec * 1_000_000L);
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage img = converter.getBufferedImage(grabber.grabImage(), 1.0);
            if (img == null) throw new IOException("failed to grab image");
            ImageIO.write(img, "jpg", thumb);
            grabber.stop();
            return thumb;
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
