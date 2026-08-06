package com.practical.leavemaster.storage;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Profile("!cloudrun")
public class LocalStorageService implements StorageService {

    private final Path rootDir;

    public LocalStorageService(@Value("${app.storage.local-dir:${java.io.tmpdir}/leavemaster-attachments}") String localDir) throws IOException {
        this.rootDir = Paths.get(localDir);
        Files.createDirectories(this.rootDir);
    }

    @Override
    public String store(String pathPrefix, MultipartFile file) throws IOException {
        String extension = StorageUtils.getExtension(file.getOriginalFilename());
        // Sanitize: strip path separators from the extension to prevent traversal
        extension = extension.replaceAll("[/\\\\]", "");
        // Sanitize: allow only alphanumerics, hyphens, and underscores in prefix
        String safePrefix = pathPrefix.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        String key = safePrefix + "/" + UUID.randomUUID() + extension;
        Path target = rootDir.resolve(key).normalize();
        if (!target.startsWith(rootDir)) {
            throw new IOException("Computed storage path escapes root directory");
        }
        Files.createDirectories(target.getParent());
        file.transferTo(target);
        return key;
    }

    @Override
    public void serve(String storageKey, HttpServletResponse response) throws IOException {
        Path filePath = rootDir.resolve(storageKey).normalize();
        if (!filePath.startsWith(rootDir)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid storage key");
            return;
        }
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Attachment not found");
            return;
        }
        String contentType = Files.probeContentType(filePath);
        if (contentType != null) {
            response.setContentType(contentType);
        }
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filePath.getFileName() + "\"");
        try (InputStream in = Files.newInputStream(filePath)) {
            in.transferTo(response.getOutputStream());
        }
    }
}
