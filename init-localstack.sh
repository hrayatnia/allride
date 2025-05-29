#!/bin/bash

# Wait for LocalStack to be ready (timeout after 180s)
echo "Waiting for LocalStack to be ready..."
TIMEOUT=180
ELAPSED=0
until curl -s http://localhost:4566/_localstack/health | tee /tmp/ls_health | grep -qE '"sqs": "(running|available)"'; do
    sleep 1
    ELAPSED=$((ELAPSED+1))
    if [ $ELAPSED -ge $TIMEOUT ]; then
        echo "LocalStack did not become ready in $TIMEOUT seconds. Health response was:"
        cat /tmp/ls_health
        exit 1
    fi
done

# Configure AWS CLI for LocalStack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566

# Create SQS queues
echo "Creating SQS queues..."
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name allride-main-queue
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name allride-dlq

echo "LocalStack initialization complete!" 