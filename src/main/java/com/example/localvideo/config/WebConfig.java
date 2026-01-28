package com.example.localvideo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.videoBaseDir}")
    private String videoBaseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + normalizeDir(videoBaseDir) + "/";
        registry.addResourceHandler("/media/**")
                .addResourceLocations(location);
        String tmp = "file:" + normalizeDir(System.getProperty("user.dir")) + "/tmp/";
        registry.addResourceHandler("/output/**")
                .addResourceLocations(tmp);
    }

    private String normalizeDir(String dir) {
        return dir.replace("\\", "/");
    }
}
