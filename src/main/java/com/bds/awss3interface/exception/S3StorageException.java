package com.bds.awss3interface.exception;

public class S3StorageException extends RuntimeException {
    public S3StorageException(String message) {
        super(message);
    }

    public S3StorageException(String message, Throwable cause) {
        super(message, cause);
    }

}
