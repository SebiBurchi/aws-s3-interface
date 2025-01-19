package com.bds.awss3interface.service.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.bds.awss3interface.common.StorageService;
import com.bds.awss3interface.exception.ResourceNotFoundException;
import com.bds.awss3interface.exception.S3StorageException;
import com.bds.awss3interface.model.ListResult;
import com.bds.awss3interface.model.Resource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Concrete strategy using AWS S3 API (works with MinIO if configured properly).
 */
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final AmazonS3 s3Client;
    private final String bucketName;

    @Override
    public ListResult<Resource> listFolder(Resource parent, String cursor) {
        String prefix = (parent != null && parent.getType() == 1) ? parent.getId() : "";

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withDelimiter("/")
                .withContinuationToken(cursor)
                .withMaxKeys(20);

        try {
            ListObjectsV2Result result = s3Client.listObjectsV2(request);
            List<Resource> resources = new ArrayList<>();

            for (String cp : result.getCommonPrefixes()) {
                String folderName = extractName(cp);
                resources.add(Resource.builder()
                        .id(cp)
                        .name(folderName)
                        .type(1)
                        .build());
            }

            for (S3ObjectSummary obj : result.getObjectSummaries()) {
                if (obj.getKey().equals(prefix)) {
                    continue; // skip the "folder placeholder" object
                }
                String fileName = extractName(obj.getKey());
                resources.add(Resource.builder()
                        .id(obj.getKey())
                        .name(fileName)
                        .type(0)
                        .build());
            }

            String nextCursor = result.getNextContinuationToken();
            return ListResult.<Resource>builder()
                    .resources(resources)
                    .cursor(nextCursor)
                    .build();

        } catch (AmazonServiceException e) {
            logger.error("S3 Service error listing folder: {}", e.getMessage());
            throw new S3StorageException("Error listing folder: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            logger.error("S3 Client error listing folder: {}", e.getMessage());
            throw new S3StorageException("AWS SDK client error: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource getResource(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Resource ID cannot be null");
        }
        int type = id.endsWith("/") ? 1 : 0;

        try {
            s3Client.getObjectMetadata(bucketName, id);
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404) {
                logger.warn("Resource not found: {}", id);
                throw new ResourceNotFoundException("Resource not found in S3: " + id, e);
            }
            logger.error("S3 error retrieving metadata for {}: {}", id, e.getMessage());
            throw new S3StorageException("Error retrieving resource: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            logger.error("SDK client error retrieving metadata for {}: {}", id, e.getMessage());
            throw new S3StorageException("Client error retrieving resource: " + e.getMessage(), e);
        }

        String name = extractName(id);
        return Resource.builder()
                .id(id)
                .name(name)
                .type(type)
                .build();
    }

    /**
     * Downloads the specified resource to a temp file and returns it.
     */
    @Override
    public File getAsFile(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }
        if (resource.getType() == 1) {
            throw new UnsupportedOperationException("Cannot download a folder as a file.");
        }

        File tempFile;
        try {
            tempFile = File.createTempFile("s3-", "-" + resource.getName());
        } catch (IOException e) {
            throw new S3StorageException("Failed to create temp file", e);
        }

        try {
            s3Client.getObject(
                    new GetObjectRequest(bucketName, resource.getId()),
                    tempFile
            );
        } catch (SdkClientException e) {
            throw new S3StorageException("Error downloading file: " + e.getMessage(), e);
        }

        return tempFile;
    }


    /**
     * Extract the final component of a key or prefix
     * e.g. "some/folder/object.txt" -> "object.txt"
     * "some/folder/" -> "folder"
     */
    private String extractName(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        String trimmed = key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
        int slashIndex = trimmed.lastIndexOf('/');
        return (slashIndex >= 0) ? trimmed.substring(slashIndex + 1) : trimmed;
    }
}
