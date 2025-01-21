package com.bds.awss3interface.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.bds.awss3interface.common.StorageService;
import com.bds.awss3interface.service.s3.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StorageConfiguration {

    @Value("${s3.endpoint}")
    private String s3Endpoint;

    @Value("${s3.region}")
    private String s3Region;

    @Value("${s3.access-key}")
    private String s3AccessKey;

    @Value("${s3.secret-key}")
    private String s3SecretKey;

    @Value("${s3.bucket}")
    private String s3Bucket;

    /**
     * Create and configure the AmazonS3 client.
     */
    @Bean
    public AmazonS3 amazonS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(s3AccessKey, s3SecretKey)))
                .withPathStyleAccessEnabled(true) // needed for MinIO; for AWS S3 you might remove it
                .build();
    }

    /**
     * Create the S3-based implementation of StorageService.
     * <p>
     * If you want to switch to a different strategy (e.g., local disk),
     * you can change this bean to return something else.
     */
    @Bean
    public StorageService storageService(AmazonS3 amazonS3Client) {
        return new S3StorageService(amazonS3Client, s3Bucket);
    }
}
