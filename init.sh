#!/bin/sh

set -e
set -x

echo "Starting init script..."
echo "Waiting for MinIO to be available..."

MAX_RETRIES=30
RETRY_COUNT=0

# Wait for MinIO to be ready using mc alias set
until mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ "$RETRY_COUNT" -ge "$MAX_RETRIES" ]; then
        echo "MinIO did not become available after $MAX_RETRIES retries. Exiting."
        exit 1
    fi
    echo "Retry $RETRY_COUNT/$MAX_RETRIES: MinIO is not ready yet..."
    sleep 2
done

echo "MinIO is now available. Proceeding with initialization..."
mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

# Create the default bucket if it doesn't exist
if mc ls local/qteam-solutions >/dev/null 2>&1; then
    echo "Bucket 'qteam-solutions' already exists."
else
    echo "Creating bucket 'qteam-solutions'..."
    mc mb local/qteam-solutions
fi

# Make the bucket public by setting the appropriate policy
echo "Setting public policy for the bucket 'qteam-solutions'..."
mc anonymous set download local/qteam-solutions

echo "Initialization completed."
