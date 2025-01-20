package com.bds.awss3interface.controller;

import com.bds.awss3interface.common.StorageService;
import com.bds.awss3interface.model.ListResult;
import com.bds.awss3interface.model.Resource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * Controller exposing endpoints for AWS S3 file and folder operations.
 * Provides APIs to list, retrieve, download, and upload files and folders.
 * Endpoints all start with "/api/s3/files".
 */
@RestController
@RequestMapping("/api/s3/files")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "Operations for managing files and folders in S3")
public class S3FileController {

    private static final Logger logger = LoggerFactory.getLogger(S3FileController.class);

    private final StorageService s3StorageService;

    /**
     * Lists the contents of a specific folder.
     * Folder keys containing slashes must be URL-encoded (e.g., "images/" -> "images%2F").
     *
     * @param folderId The ID (S3 key) of the folder to list.
     * @param cursor   Optional pagination token to retrieve the next set of results.
     * @return A {@link ListResult} containing the folder contents and a continuation token for pagination.
     */
    @GetMapping("/list/folder")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "List folder contents", security = @SecurityRequirement(name = "basicAuth"))
    public ListResult<Resource> listFolder(
            @RequestParam @Parameter(description = "The S3 key of the folder to list") String folderId,
            @RequestParam(required = false) @Parameter(description = "Pagination cursor for retrieving the next set of results") String cursor) {
        logger.info("Listing contents of folder: {} with cursor: {}", folderId, cursor);
        Resource folderResource = s3StorageService.getResource(folderId);
        return s3StorageService.listFolder(folderResource, cursor);
    }

    /**
     * Retrieves metadata for a specific file or folder.
     *
     * @param id The S3 key of the file or folder.
     * @return A {@link Resource} object containing the metadata of the file or folder.
     */
    @GetMapping("/resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Retrieve resource metadata", security = @SecurityRequirement(name = "basicAuth"))
    public Resource getResource(
            @RequestParam @Parameter(description = "The S3 key of the file or folder to retrieve metadata for") String id) {
        logger.info("Retrieving metadata for resource: {}", id);
        return s3StorageService.getResource(id);
    }

    /**
     * Downloads a file and returns it as an attachment.
     *
     * @param id The S3 key of the file to download.
     * @return A {@link ResponseEntity} containing the file as an attachment.
     */
    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Download a file", security = @SecurityRequirement(name = "basicAuth"))
    public ResponseEntity<FileSystemResource> downloadFile(
            @RequestParam @Parameter(description = "The S3 key of the file to download") String id) {
        logger.info("Downloading file with key: {}", id);

        Resource resource = s3StorageService.getResource(id);
        File file = s3StorageService.getAsFile(resource);
        FileSystemResource fsResource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getName() + "\"")
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fsResource);
    }

    /**
     * Uploads a file to S3/MinIO at the specified key.
     * Only accessible to users with the ADMIN role.
     *
     * @param file The file to upload.
     * @param key  The S3 key under which the file should be stored.
     * @return A success message indicating the file has been uploaded.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a file", security = @SecurityRequirement(name = "basicAuth"))
    public String uploadFile(
            @RequestParam("file") @Parameter(description = "The file to upload") MultipartFile file,
            @RequestParam("key") @Parameter(description = "The S3 key under which the file will be stored") String key) {
        logger.info("Uploading file with key: {}", key);
        s3StorageService.uploadFile(key, file);
        return "File uploaded successfully with key: " + key;
    }
}
