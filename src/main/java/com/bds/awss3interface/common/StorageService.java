package com.bds.awss3interface.common;

import com.bds.awss3interface.model.ListResult;
import com.bds.awss3interface.model.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface StorageService {
    /**
     * Lists the contents of a given parent resource, with optional pagination.
     *
     * @param parent The parent folder resource (null = root).
     * @param cursor The pagination cursor from a previous call (null if first call).
     * @return A ListResult containing the found resources and a new cursor (if any).
     */
    ListResult<Resource> listFolder(Resource parent, String cursor);

    /**
     * Retrieves metadata for a specific resource by its ID (key).
     *
     * @param id The resource ID (S3 key, local path, etc.); cannot be null.
     * @return The Resource object if found.
     */
    Resource getResource(String id);

    /**
     * Downloads a resource (if it is a file) and returns a local File reference.
     *
     * @param resource The resource to download (cannot be null, must be file).
     * @return A File containing the downloaded contents.
     */
    File getAsFile(Resource resource);


    /**
     * Uploads a file to S3/MinIO at the given key.
     *
     * @param key  the S3 object key (e.g. "uploads/file.txt")
     * @param file the multipart file received from the client
     */
    void uploadFile(String key, MultipartFile file);
}
