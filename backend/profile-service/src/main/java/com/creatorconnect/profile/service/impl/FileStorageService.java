package com.creatorconnect.profile.service.impl;

import com.creatorconnect.profile.exception.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Handles file storage operations for profile pictures.
 *
 * <p>Stores uploaded images on the local filesystem under a configurable
 * directory. Each file is renamed to a UUID to avoid collisions and
 * overwrites. Old files are replaced when a new upload arrives.
 *
 * <p>File type validation enforces an allow-list of image extensions
 * (jpg, jpeg, png, webp) and a configurable maximum size.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** Allowed image file extensions (case-insensitive). */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Path storageLocation;

    /**
     * Creates the service and ensures the storage directory exists.
     *
     * @param uploadDir the configured upload directory path
     */
    public FileStorageService(@Value("${app.upload.dir:./uploads/profile-pictures}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
            log.info("Profile picture storage directory: {}", this.storageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory: " + this.storageLocation, ex);
        }
    }

    /**
     * Validates and stores an uploaded file, returning the relative path
     * suitable for serving back to the client.
     *
     * @param file the uploaded multipart file
     * @return the relative path (e.g. {@code profile-pictures/abc123.jpg})
     * @throws InvalidFileException when the file is empty, has an invalid
     *         extension, or exceeds the maximum size
     */
    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );
        String extension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID() + "." + extension;

        try {
            Path targetLocation = this.storageLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = "profile-pictures/" + filename;
            log.info("Stored profile picture: {}", relativePath);
            return relativePath;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + filename, ex);
        }
    }

    /**
     * Deletes a previously stored file. Silently ignores missing files.
     *
     * @param relativePath the relative path returned by {@link #storeFile}
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path filePath = this.storageLocation.resolve(
                    relativePath.replace("profile-pictures/", "")
            );
            Files.deleteIfExists(filePath);
            log.info("Deleted profile picture: {}", relativePath);
        } catch (IOException ex) {
            log.warn("Could not delete file: {}", relativePath, ex);
        }
    }

    /**
     * Resolves a relative path to an absolute filesystem path for serving.
     *
     * @param relativePath the relative path (e.g. {@code profile-pictures/abc123.jpg})
     * @return the absolute, normalized path
     */
    public Path resolvePath(String relativePath) {
        return this.storageLocation.resolve(
                relativePath.replace("profile-pictures/", "")
        ).normalize();
    }

    /**
     * Validates the uploaded file against type and size constraints.
     *
     * @param file the uploaded file
     * @throws InvalidFileException when validation fails
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty. Please select an image to upload.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : ""
        );
        String extension = getFileExtension(originalFilename).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                    "Invalid file type. Allowed types: JPG, JPEG, PNG, WEBP."
            );
        }

        // 10 MB limit
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new InvalidFileException(
                    "File is too large. Maximum size is 10 MB."
            );
        }
    }

    /**
     * Extracts the file extension (without the dot) from a filename.
     *
     * @param filename the original filename
     * @return the extension in lowercase, or empty string if none
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
