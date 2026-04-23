package com.driveme.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves the entire configured upload root (default {@code src/main/resources/static/uploads})
 * at {@code /uploads/**}. That includes {@code uploads/drivers/*} (license, criminal record) and
 * files at {@code uploads/*} (e.g. profile selfies) in one tree — no extra handler needed per subfolder.
 * <p>CORS for {@code /uploads/**} is registered in {@link CorsConfig} (admin on port 5173).
 */
@Configuration
public class WebMvcUploadConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:src/main/resources/static/uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ignored) {
            // Directory may be created on first upload instead
        }
        String location = "file:" + root + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
