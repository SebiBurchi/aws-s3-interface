package com.bds.awss3interface.controller;

import com.bds.awss3interface.common.StorageService;
import com.bds.awss3interface.model.ListResult;
import com.bds.awss3interface.model.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;

/**
 * A dedicated controller exposing AWS S3 file/folder operations.
 * Endpoints all start with /api/s3/files.
 */
@RestController
@RequestMapping("/api/s3/files")
@RequiredArgsConstructor
public class S3FileController {
    private final StorageService s3StorageService;

    /**
     * Lists the contents at the "root" (no parent).
     * Pagination is supported via an optional 'cursor'.
     * <p>
     * Example: GET /api/s3/files/list
     */
    @GetMapping("/list")
    public ListResult<Resource> listRoot(@RequestParam(required = false) String cursor) {
        return s3StorageService.listFolder(null, cursor);
    }

    /**
     * Lists the contents of a specific folder (identified by folderId).
     * Folder IDs with slashes need to be URL-encoded (e.g. "images/" -> "images%2F").
     * Pagination is supported via an optional 'cursor'.
     * <p>
     * Example: GET /api/s3/files/list/images%2F
     */
    @GetMapping("/list/{folderId}")
    public ListResult<Resource> listFolder(@PathVariable String folderId,
                                           @RequestParam(required = false) String cursor) {

        Resource folderResource = s3StorageService.getResource(folderId);
        return s3StorageService.listFolder(folderResource, cursor);
    }

    /**
     * Retrieves metadata for a file or folder by its S3 key (id).
     * <p>
     * Example: GET /api/s3/files/resource/myfile.txt
     */
    @GetMapping("/resource/{id}")
    public Resource getResource(@PathVariable String id) {
        return s3StorageService.getResource(id);
    }

    /**
     * Downloads a file resource and returns it.
     * <p>
     * Example: GET /api/s3/files/download/myfile.txt
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/download/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<FileSystemResource> downloadFile(@PathVariable String id) {
        Resource resource = s3StorageService.getResource(id);
        File file = s3StorageService.getAsFile(resource);

        FileSystemResource fsResource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getName() + "\"")
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fsResource);
    }
}
