# AWS S3 Interface

This is a **Spring Boot** application that provides a **REST API** for interacting with **AWS S3** or **MinIO**. The application enables file and folder operations such as listing, retrieving, and downloading objects. It uses **OpenAPI** for API documentation and integrates with **Spring Security** for optional authentication and role-based access control.

## Features
- **REST API**: Exposes endpoints for file and folder operations.
- **MinIO Integration**: Uses a MinIO container as the default storage backend.
- **OpenAPI Documentation**: Explore APIs through Swagger UI at `http://localhost:8080/swagger-ui.html`.
- **Makefile Automation**: Simplifies common development tasks.

## Prerequisites
Before running the application, ensure the following tools are installed: **Java 17**, **Maven**, and **Docker**.

## Quick Start
To get started with the application, follow these steps:
1. **Build the Application**: Run the following command to package the application into a JAR file: `make build`.
2. **Start Supporting Services (MinIO)**: Use Docker Compose to start the MinIO container: `make docker-up`. MinIO details: Console: `http://localhost:9001`, API: `http://localhost:9000`. Credentials: Username: `admin`, Password: `admin123`. A default bucket named `qteam-solutions` is automatically created during initialization.
3. **Run the Application**: Start the Spring Boot application locally: `make run`. Application details: Base URL: `http://localhost:8080`. API Documentation: `http://localhost:8080/swagger-ui.html`.
4. **Shut Down Services**: To stop the application and supporting services, run: `make docker-down`. This will stop and remove all containers, including MinIO.

## Makefile Commands
The following commands are available in the included `Makefile` for automating common tasks:
- `make all`: Builds the application, starts supporting services (e.g., MinIO), and runs the application in the `dev` profile.
- `make build`: Cleans and packages the application into a JAR file.
- `make test`: Runs all unit and integration tests.
- `make run`: Starts the application locally using the generated JAR file.
- `make docker-up`: Starts MinIO and other supporting services via Docker Compose.
- `make docker-down`: Stops and removes MinIO and associated containers.
- `make clean`: Cleans up Maven build artifacts.

## Integration with MinIO
This application is configured to work with **MinIO** as a local S3-compatible object storage solution. MinIO is used for testing and development purposes and is initialized with a default bucket (`qteam-solutions`).

## API Endpoints
- **GET** `/api/s3/files/list/folder`: Lists the contents of a folder in the bucket.
- **GET** `/api/s3/files/resource`: Retrieves metadata for a file or folder.
- **GET** `/api/s3/files/download`: Downloads a file as an attachment.
- **POST** `/api/s3/files/upload`: Uploads a file to the bucket (ADMIN only).

## OpenAPI Documentation
API documentation is available via Swagger UI at: `http://localhost:8080/swagger-ui.html`. Use this interface to explore, test, and debug the application's endpoints.

