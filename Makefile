# Use bash for shell features if needed
SHELL := /bin/bash

# Declare these targets phony (they don't produce real files)
.PHONY: build test run run-prod docker-up docker-down clean

# 1) Build your Spring Boot application using Maven
build:
	mvn clean package

# 2) Run tests
test:
	mvn test

# 3) Run the Spring Boot application locally (dev profile by default)
run:
	java -jar target/my-application-1.0.0.jar

# 3b) Run the Spring Boot application using the 'prod' profile
run-prod:
	java -jar target/my-application-1.0.0.jar --spring.profiles.active=prod

# 4) Spin up supporting services (e.g., MinIO) via docker-compose
docker-up:
	docker-compose up -d

# 5) Shut down docker-compose services
docker-down:
	docker-compose down

# 6) Clean up any artifacts
clean:
	mvn clean
