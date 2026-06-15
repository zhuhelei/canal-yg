package com.canal.dronechange.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath();
        Files.createDirectories(uploadRoot);
    }

    public String store(MultipartFile file, String month) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        Path monthDir = uploadRoot.resolve(month);
        Files.createDirectories(monthDir);
        String fileName = UUID.randomUUID() + extension;
        Path target = monthDir.resolve(fileName);
        file.transferTo(target);
        return "/uploads/" + month + "/" + fileName;
    }

    public String storeBytes(byte[] bytes, String month, String originalPath) throws IOException {
        String extension = getExtension(originalPath);
        Path monthDir = uploadRoot.resolve(month);
        Files.createDirectories(monthDir);
        String fileName = UUID.randomUUID() + extension;
        Path target = monthDir.resolve(fileName);
        Files.write(target, bytes);
        return "/uploads/" + month + "/" + fileName;
    }

    public boolean isImagePath(String path) {
        String lowerPath = path.toLowerCase(Locale.ROOT);
        return lowerPath.endsWith(".jpg")
                || lowerPath.endsWith(".jpeg")
                || lowerPath.endsWith(".png")
                || lowerPath.endsWith(".tif")
                || lowerPath.endsWith(".tiff")
                || lowerPath.endsWith(".webp");
    }

    private String getExtension(String path) {
        if (path == null) {
            return "";
        }
        String cleanPath = StringUtils.cleanPath(path);
        int slashIndex = Math.max(cleanPath.lastIndexOf('/'), cleanPath.lastIndexOf('\\'));
        String name = slashIndex >= 0 ? cleanPath.substring(slashIndex + 1) : cleanPath;
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex >= 0) {
            return name.substring(dotIndex);
        }
        return "";
    }
}
