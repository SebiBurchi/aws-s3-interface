package com.bds.awss3interface.integration;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests.
 * Sets up a MinIO container and configures Spring properties accordingly.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    private AmazonS3 amazonS3;

    static MinIOContainer minioContainer = new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
            .withUserName("admin")
            .withPassword("admin123");

    @BeforeAll
    static void setUp() {
        minioContainer.start();
    }

    @AfterAll
    static void tearDown() {
        minioContainer.stop();
    }

    /**
     * Ensures the S3/MinIO bucket exists before each test.
     */
    @BeforeEach
    void ensureBucketExists() {
        if (!amazonS3.doesBucketExistV2("qteam-solutions")) {
            amazonS3.createBucket("qteam-solutions");
        }
    }

    /**
     * Dynamically registers properties for the Spring context based on the MinIO container.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("s3.endpoint", minioContainer::getS3URL);
        registry.add("s3.access-key", minioContainer::getUserName);
        registry.add("s3.secret-key", minioContainer::getPassword);
        registry.add("s3.bucket", () -> "qteam-solutions");
    }
}
