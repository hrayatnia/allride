# AllRide Backend Service

A Kotlin-based backend service that provides gRPC endpoints for user management and asynchronous CSV processing capabilities. This service is part of the AllRide User Management System.

## Quick Start

### Using Docker (Recommended)
The easiest way to run this service is through Docker, which is set up in the parent repository.
See the main repository's README for Docker setup instructions.

### Manual Setup

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

## Project Structure

```
src/
├── main/
│   ├── kotlin/
│   │   └── me/rayatnia/
│   │       ├── api/           # gRPC service implementations
│   │       ├── application/   # Application services
│   │       ├── domain/        # Domain models and logic
│   │       └── infrastructure/# External services integration
│   └── proto/                 # Protocol buffer definitions
└── test/                      # Test cases
```

## Development

### Building
```bash
./gradlew build
```

### Running Tests
```bash
./gradlew test
```

### Generating Protocol Buffers
```bash
./gradlew generateProto
```

## Docker Support

### Building the Image
```bash
docker build -t allride-backend .
```

### Running with Docker
```bash
docker run -p 50051:50051 allride-backend
```

## API Documentation

### Generating Documentation
```bash
./gradlew generateProtoDocs
```

Documentation will be available in:
- HTML: `docs/grpc-api.html`
- Markdown: `docs/grpc-api.md`

## Infrastructure

### AWS Services
- SQS for message queuing
- Optional: S3 for file storage
- Optional: RDS for persistent storage

### Local Development
Use LocalStack for AWS service emulation:
```bash
./init-localstack.sh
```

## Monitoring

### Health Checks
- gRPC health check: port 50051
- HTTP health check: `/health`

### Metrics
- JVM metrics
- Custom business metrics
- gRPC server metrics

## Security

### SSL/TLS
- Configure using `keystore.jks`
- Set keystore password in environment

### Authentication
- gRPC interceptors for auth
- JWT token validation

## Troubleshooting

### Common Issues

1. LocalStack Connection
   ```bash
   aws --endpoint-url=http://localhost:4566 sqs list-queues
   ```

2. Memcached Connection
   ```bash
   echo 'stats' | nc localhost 11211
   ```

3. Port Conflicts
   ```bash
   lsof -i :50051
   ```

## Contributing

1. Create a feature branch
2. Add tests
3. Update documentation
4. Submit a pull request

See the main repository's CONTRIBUTING.md for more details.

