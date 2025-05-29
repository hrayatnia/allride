#!/bin/bash

# Wait for LocalStack to be ready
echo "Waiting for LocalStack to be ready..."
while ! curl -s "http://localhost:4566/_localstack/health" | grep -q "\"sqs\": \"running\""; do
    sleep 1
done

# Create SQS queues
echo "Creating SQS queues..."
awslocal sqs create-queue --queue-name allride-main-queue
awslocal sqs create-queue --queue-name allride-dlq

echo "LocalStack initialization complete!" 