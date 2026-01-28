package com.example.localvideo.model;

public class VideoFile {
    private String name;
    private String relativePath;
    private long size;

    public VideoFile() {}

    public VideoFile(String name, String relativePath, long size) {
        this.name = name;
        this.relativePath = relativePath;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
