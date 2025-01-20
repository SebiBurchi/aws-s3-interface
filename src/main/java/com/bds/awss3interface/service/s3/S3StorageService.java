package com.bds.awss3interface.service.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.bds.awss3interface.common.StorageService;
import com.bds.awss3interface.exception.ResourceNotFoundException;
import com.bds.awss3interface.exception.S3StorageException;
import com.bds.awss3interface.model.ListResult;
import com.bds.awss3interface.model.Resource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);
    private final AmazonS3 s3Client;
    private final String bucketName;
    private final int PAGE_SIZE = 20;

    @Override
    public ListResult<Resource> listFolder(Resource parent, String cursor) {
        String prefix = (parent != null && parent.getType() == 1) ? parent.getId() : "";

        logger.info("Listing folder with prefix: '{}' and cursor: '{}'", prefix, cursor);

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withDelimiter("/")
                .withContinuationToken(cursor)
                .withMaxKeys(PAGE_SIZE);

        try {
            ListObjectsV2Result result = s3Client.listObjectsV2(request);
            List<Resource> resources = new ArrayList<>();

            // Process folder prefixes
            result.getCommonPrefixes().forEach(prefixKey -> {
                resources.add(createResource(prefixKey, 1)); // 1 = folder
            });

            // Process file objects
            result.getObjectSummaries().forEach(obj -> {
                if (!obj.getKey().equals(prefix)) { // Skip folder placeholder object
                    resources.add(createResource(obj.getKey(), 0)); // 0 = file
                }
            });

            String nextCursor = result.getNextContinuationToken();
            logger.info("Successfully listed folder. Found {} resources. Next cursor: {}", resources.size(), nextCursor);

            return ListResult.<Resource>builder()
                    .resources(resources)
                    .cursor(nextCursor)
                    .build();

        } catch (AmazonServiceException e) {
            logger.error("Error listing folder in bucket '{}': {}", bucketName, e.getMessage());
            throw new S3StorageException("Error listing folder: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            logger.error("SDK client error while listing folder in bucket '{}': {}", bucketName, e.getMessage());
            throw new S3StorageException("AWS SDK client error: " + e.getMessage(), e);
        }
    }

    @Override
    public File getAsFile(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }

        if (resource.getType() == 1) {
            throw new UnsupportedOperationException("Cannot download a folder as a file.");
        }

        logger.info("Downloading resource '{}' as file", resource.getId());

        File tempFile;
        try {
            tempFile = File.createTempFile("s3-", "-" + resource.getName());
        } catch (IOException e) {
            logger.error("Failed to create temporary file for resource '{}'", resource.getId(), e);
            throw new S3StorageException("Failed to create temp file", e);
        }

        try {
            s3Client.getObject(new GetObjectRequest(bucketName, resource.getId()), tempFile);
            logger.info("Successfully downloaded resource '{}' to '{}'", resource.getId(), tempFile.getAbsolutePath());
        } catch (SdkClientException e) {
            logger.error("Error downloading file '{}': {}", resource.getId(), e.getMessage());
            throw new S3StorageException("Error downloading file: " + e.getMessage(), e);
        }

        return tempFile;
    }

    @Override
    public void uploadFile(String key, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        logger.info("Uploading file to bucket '{}' with key '{}'", bucketName, key);

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(bucketName, key, file.getInputStream(), metadata);
            logger.info("Successfully uploaded file with key '{}'", key);

        } catch (IOException e) {
            logger.error("Error reading file for upload with key '{}'", key, e);
            throw new S3StorageException("Failed to read uploaded file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error uploading file with key '{}': {}", key, e.getMessage());
            throw new S3StorageException("Error uploading file to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource getResource(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource ID cannot be null or empty");
        }

        int type = id.endsWith("/") ? 1 : 0; // Determine if folder (1) or file (0)
        boolean exists;

        logger.info("Checking existence of resource '{}' in bucket '{}'", id, bucketName);

        try {
            exists = (type == 0) ? s3Client.doesObjectExist(bucketName, id) : doesFolderExist(bucketName, id);

            if (!exists) {
                logger.warn("Resource '{}' not found in bucket '{}'", id, bucketName);
                throw new ResourceNotFoundException("Resource not found in bucket '" + bucketName + "' with ID: " + id);
            }

            logger.info("Resource '{}' exists in bucket '{}'", id, bucketName);

        } catch (AmazonServiceException e) {
            logger.error("AWS service error while retrieving resource '{}': {}", id, e.getMessage());
            throw new S3StorageException("AWS Service error: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            logger.error("SDK client error while retrieving resource '{}': {}", id, e.getMessage());
            throw new S3StorageException("AWS SDK client error: " + e.getMessage(), e);
        }

        String name = extractName(id);
        return Resource.builder()
                .id(id)
                .name(name)
                .type(type)
                .build();
    }

    private boolean doesFolderExist(String bucketName, String folderKey) {
        String normalizedFolderKey = folderKey.endsWith("/") ? folderKey : folderKey + "/";

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(normalizedFolderKey)
                .withMaxKeys(1);

        ListObjectsV2Result result = s3Client.listObjectsV2(request);

        return !result.getObjectSummaries().isEmpty() || !result.getCommonPrefixes().isEmpty();
    }

    private Resource createResource(String key, int type) {
        return Resource.builder()
                .id(key)
                .name(extractName(key))
                .type(type)
                .build();
    }

    private String extractName(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        String trimmed = key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
        int slashIndex = trimmed.lastIndexOf('/');
        return (slashIndex >= 0) ? trimmed.substring(slashIndex + 1) : trimmed;
    }
}
