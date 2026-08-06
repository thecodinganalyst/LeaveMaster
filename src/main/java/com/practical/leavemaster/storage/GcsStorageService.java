package com.practical.leavemaster.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Profile("cloudrun")
public class GcsStorageService implements StorageService {

    private final Storage storage;
    private final String bucketName;

    public GcsStorageService(@Value("${app.storage.gcs-bucket}") String bucketName) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucketName = bucketName;
    }

    @Override
    public String store(String applicationId, MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String objectName = applicationId + "/" + UUID.randomUUID() + extension;
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());
        return objectName;
    }

    @Override
    public void serve(String storageKey, HttpServletResponse response) throws IOException {
        BlobId blobId = BlobId.of(bucketName, storageKey);
        String signedUrl = storage.signUrl(
                BlobInfo.newBuilder(blobId).build(),
                15, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.withV4Signature()
        ).toString();
        response.sendRedirect(signedUrl);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
