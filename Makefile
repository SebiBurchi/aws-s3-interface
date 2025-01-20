SHELL := /bin/bash

.PHONY: all build test run run-dev run-prod docker-up docker-down clean help api-docs

# Help menu to guide the client on how to use this Makefile
help:
	@echo "Usage:"
	@echo "  make all            - Build, start services, and run the application"
	@echo "  make build          - Build the Spring Boot application"
	@echo "  make test           - Run tests"
	@echo "  make run-dev        - Run the application with the 'dev' profile"
	@echo "  make run-prod       - Run the application with the 'prod' profile"
	@echo "  make docker-up      - Start supporting services (e.g., MinIO)"
	@echo "  make docker-down    - Stop supporting services"
	@echo "  make clean          - Clean up build artifacts"
	@echo "  make api-docs       - Print example API calls for testing"

# 0) Run everything to get the app fully operational
all: build docker-up run-dev
	@echo "Application is fully operational!"

# 1) Build the Spring Boot application using Maven
build:
	@echo "Building the application..."
	mvn clean package -DskipTests
	@echo "Build completed successfully."

# 2) Run tests
test:
	@echo "Running tests..."
	mvn test -Dspring.config.location=src/test/resources/application-test.properties
	@echo "All tests completed successfully."

# 3) Run the Spring Boot application locally (dev profile by default)
run-dev:
	@echo "Starting the application in 'dev' mode..."
	java -jar target/aws-s3-interface-0.0.1.jar --spring.profiles.active=dev

# 3b) Run the Spring Boot application using the 'prod' profile
run-prod:
	@echo "Starting the application in 'prod' mode..."
	java -jar target/aws-s3-interface-0.0.1.jar --spring.profiles.active=prod

# 4) Start supporting services (e.g., MinIO) using docker-compose
docker-up:
	@echo "Setting executable permissions for init.sh..."
	chmod +x init.sh
	@echo "Starting supporting services using Docker Compose..."
	docker-compose up -d
	@echo "Services started successfully."

# 5) Shut down supporting services
docker-down:
	@echo "Stopping supporting services..."
	docker-compose down --volumes
	@echo "Services stopped successfully."

# 6) Clean up any build artifacts
clean:
	@echo "Cleaning up build artifacts..."
	mvn clean
	@echo "Clean-up completed successfully."

# 7) Print example API calls for testing the application
api-docs:
	@echo "Example API calls:"
	@echo "  1. List folder contents:"
	@echo "     curl -X GET 'http://localhost:8080/api/s3/files/list/folder?folderId=your-folder-id'"
	@echo "  2. Retrieve resource metadata:"
	@echo "     curl -X GET 'http://localhost:8080/api/s3/files/resource?id=your-file-id'"
	@echo "  3. Download a file:"
	@echo "     curl -X GET 'http://localhost:8080/api/s3/files/download?id=your-file-id' -o downloaded-file"
	@echo "  4. Upload a file:"
	@echo "     curl -X POST -F 'file=@path-to-your-file' -F 'key=your-s3-key' 'http://localhost:8080/api/s3/files/upload'"
