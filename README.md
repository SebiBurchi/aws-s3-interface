# AWS S3 Interface

This project provides a **Spring Boot** application for interacting with **AWS S3** (or **MinIO**) to list, retrieve, and download files. It includes:

- A **REST API** exposing endpoints for folder listing, file retrieval, and downloads.
- **Spring Security** integration for optional authentication and authorization.
- A **Makefile** to simplify common tasks (build, test, run, etc.).

## Prerequisites

- **Java 17**
- **Maven**
- **Docker**

## Makefile Commands

- **make build**  
  Cleans and packages the application.
- **make test**  
  Runs all tests.
- **make run**  
  Starts the application locally from the generated JAR.
- **make docker-up**  
  Spins up supporting services (e.g., MinIO) via Docker Compose.
- **make docker-down**  
  Shuts down Docker Compose services.
- **make clean**  
  Cleans Maven artifacts.

