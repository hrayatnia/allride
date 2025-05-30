# AllRide gRPC API Documentation

This directory contains the automatically generated documentation for AllRide's gRPC services.

## Available Documentation Formats

- [HTML Documentation](grpc-api.html) - Interactive HTML documentation of the gRPC services
- [Markdown Documentation](grpc-api.md) - Markdown version of the gRPC service documentation

## Services Overview

The AllRide application provides the following gRPC services:

### UserService

The UserService handles user data management operations:

- `UploadUserData` - Upload user data from a CSV file
- `GetUserById` - Retrieve a user by their ID
- `GetUserByEmail` - Retrieve a user by their email address
- `GetAllUsers` - Retrieve all users in the system

## How to Generate Documentation

The documentation is automatically generated during the build process. To manually regenerate the documentation, run:

```bash
./gradlew generateProto
```

This will create both HTML and Markdown versions of the API documentation in this directory.

## Using the gRPC Services

The gRPC server runs on port 50051 by default. You can change this by setting the `GRPC_PORT` environment variable.

### gRPC-Web Support

The service supports gRPC-Web through an Envoy proxy, making it accessible to web browsers. The gRPC-Web endpoint is available at:

```
http://localhost:8090
```

Example using gRPC-Web with JavaScript/TypeScript:

```typescript
import { UserServiceClient } from './generated/user_service_grpc_web_pb';
import { GetUserByIdRequest } from './generated/user_service_pb';

const client = new UserServiceClient('http://localhost:8090');
const request = new GetUserByIdRequest();
request.setId('user-id-here');

client.getUserById(request, {}, (err, response) => {
  if (err) {
    console.error('Error:', err);
    return;
  }
  console.log('User:', response.getUser().toObject());
});
```

### Example Usage with grpcurl

Here are some examples of how to interact with the gRPC services using grpcurl:

1. List all available services:
```bash
grpcurl -plaintext localhost:50051 list
```

2. Get all users:
```bash
grpcurl -plaintext localhost:50051 me.rayatnia.proto.UserService/GetAllUsers '{}'
```

3. Get user by ID:
```bash
grpcurl -plaintext localhost:50051 me.rayatnia.proto.UserService/GetUserById '{"id": "user-id-here"}'
```

4. Get user by email:
```bash
grpcurl -plaintext localhost:50051 me.rayatnia.proto.UserService/GetUserByEmail '{"email": "user@example.com"}'
```

### Client Libraries

The gRPC service definitions can be used to generate client libraries in various programming languages. The generated stubs are available in the following locations:

- Kotlin: `build/generated/source/proto/main/kotlin`
- Java: `build/generated/source/proto/main/java`
- gRPC Kotlin: `build/generated/source/proto/main/grpckt`
- gRPC Java: `build/generated/source/proto/main/grpc`

For web clients, you'll need to generate gRPC-Web code using the protoc plugin:

```bash
protoc -I=proto \
  --js_out=import_style=commonjs:generated \
  --grpc-web_out=import_style=commonjs,mode=grpcwebtext:generated \
  proto/user_service.proto
```

## Error Handling

The services use standard gRPC status codes for error handling:

- `NOT_FOUND` - When a requested resource doesn't exist
- `INVALID_ARGUMENT` - When the request parameters are invalid
- `INTERNAL` - For internal server errors
- `UNAUTHENTICATED` - When authentication is required but not provided
- `PERMISSION_DENIED` - When the authenticated user doesn't have sufficient permissions 