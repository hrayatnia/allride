# User Data Import Service

A service that handles CSV file uploads containing user data, processes them asynchronously using AWS SQS, and provides a REST API for querying the processed data.

## Architecture

This service follows several modern architectural patterns:

1. **CQRS (Command Query Responsibility Segregation)**
   - Commands: File upload handling
   - Queries: User data retrieval endpoints

2. **Event Sourcing**
   - Events are published for file uploads and processing results
   - Enables tracking of processing status and error handling

3. **Outbox Pattern**
   - Events are published atomically with state changes
   - Ensures consistency between data storage and event publishing

4. **Choreography over Orchestration**
   - Services react to events rather than being orchestrated
   - CSV processor listens for FileUploadedEvent
   - Clients can listen for UserDataProcessedEvent

## API Endpoints

### File Upload
```http
POST /api/v1/users/upload
Content-Type: multipart/form-data

file: CSV file
```

### Query Endpoints
```http
GET /api/v1/users
GET /api/v1/users/{id}
GET /api/v1/users/email/{email}
```

## CSV Format

The CSV file should have the following headers:
- firstName
- lastName
- email

Example:
```csv
firstName,lastName,email
John,Doe,john.doe@example.com
Jane,Smith,jane.smith@example.com
```

## Running the Service

### Prerequisites
- Java 17 or later
- AWS SQS (or LocalStack for development)
- Memcached

### Configuration

The service can be configured using either YAML configuration or environment variables:

```yaml
aws:
  region: us-east-1
  sqs:
    queue_url: http://localhost:4566/000000000000/allride-main-queue
    dlq_url: http://localhost:4566/000000000000/allride-dlq

memcached:
  endpoint: localhost:11211
```

Or environment variables:
```bash
AWS_REGION=us-east-1
SQS_QUEUE_URL=http://localhost:4566/000000000000/allride-main-queue
SQS_DLQ_URL=http://localhost:4566/000000000000/allride-dlq
MEMCACHED_ENDPOINT=localhost:11211
```

### Development Setup

1. Start LocalStack:
```bash
docker run -d -p 4566:4566 -e SERVICES=sqs localstack/localstack
```

2. Initialize LocalStack:
```bash
./init-localstack.sh
```

3. Run the service:
```bash
./gradlew run
```

### Running Tests

```bash
./gradlew test
```

## Error Handling

The service implements robust error handling:

1. **File Upload**
   - Validates file type (must be CSV)
   - Ensures file is not empty
   - Returns appropriate HTTP status codes

2. **CSV Processing**
   - Continues processing on row errors
   - Publishes events for both successful and failed rows
   - Maintains processing state in case of failures

## Design Decisions

1. **In-Memory Storage**
   - For this challenge, an in-memory repository is used
   - Can be easily replaced with a persistent storage implementation

2. **Event-Driven Processing**
   - Asynchronous processing using AWS SQS
   - Enables scaling and resilience
   - Supports retry mechanisms through DLQ

3. **Domain-Driven Design**
   - Clear separation of domain, application, and infrastructure layers
   - Rich domain model with validation
   - Event-sourced state changes

## Future Improvements

1. **Persistence**
   - Add a proper database implementation
   - Implement the Outbox pattern for event publishing

2. **Monitoring**
   - Add metrics collection
   - Implement health checks
   - Add tracing

3. **Security**
   - Add authentication
   - Implement rate limiting
   - Add input sanitization

4. **Performance**
   - Add caching for queries
   - Implement batch processing for large files
   - Add pagination for query endpoints

