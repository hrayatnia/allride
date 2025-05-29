#!/bin/bash

# Create main queue
awslocal sqs create-queue --queue-name allride-main-queue

# Create DLQ
awslocal sqs create-queue --queue-name allride-dlq

# List queues to verify
awslocal sqs list-queues 