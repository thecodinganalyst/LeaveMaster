package com.practical.leavemaster.storage;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    /**
     * Store the given file and return the storage key used to retrieve or access it later.
     * The key is scoped under the given applicationId so files are organized per application.
     * Keys are not easily guessable (UUID-based).
     *
     * @param applicationId the leave application ID used as a path prefix
     * @param file          the uploaded file
     * @return an opaque storage key stored in the database
     * @throws IOException if the file cannot be stored
     */
    String store(String applicationId, MultipartFile file) throws IOException;

    /**
     * Serve the file identified by storageKey to the HTTP response.
     * Implementations may stream the file bytes directly (local) or redirect to a
     * short-lived signed URL (GCS).
     *
     * @param storageKey the key previously returned by {@link #store}
     * @param response   the outgoing HTTP response
     * @throws IOException if the file cannot be retrieved or written
     */
    void serve(String storageKey, HttpServletResponse response) throws IOException;
}
