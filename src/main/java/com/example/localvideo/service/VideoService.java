package com.example.localvideo.service;

import com.example.localvideo.model.VideoFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Value("${app.videoBaseDir}")
    private String videoBaseDir;

    private final Set<String> videoExts = new HashSet<>(Arrays.asList(
            "mp4","mkv","avi","mov","webm","flv","m4v","wmv","ts","3gp"
    ));

    private volatile List<VideoFile> cache = Collections.emptyList();

    public synchronized void rescan() {
        File base = new File(videoBaseDir);
        List<VideoFile> results = new ArrayList<>();
        scanDir(base, base, results);
        // sort by name
        results.sort(Comparator.comparing(VideoFile::getName, String.CASE_INSENSITIVE_ORDER));
        cache = results;
    }

    public List<VideoFile> getAll() {
        if (cache.isEmpty()) {
            rescan();
        }
        return cache;
    }

    public List<VideoFile> getPage(int page, int pageSize) {
        List<VideoFile> all = getAll();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        if (from >= to) {
            return Collections.emptyList();
        }
        return all.subList(from, to);
    }

    private void scanDir(File base, File dir, List<VideoFile> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDir(base, f, out);
            } else if (isVideo(f)) {
                String rel = base.toPath().relativize(f.toPath()).toString().replace("\\","/");
                out.add(new VideoFile(f.getName(), rel, f.length()));
            }
        }
    }

    private boolean isVideo(File file) {
        String name = file.getName();
        int idx = name.lastIndexOf('.');
        if (idx < 0) return false;
        String ext = name.substring(idx + 1).toLowerCase(Locale.ROOT);
        return videoExts.contains(ext);
    }

    public long totalCount() {
        return getAll().size();
    }
}
