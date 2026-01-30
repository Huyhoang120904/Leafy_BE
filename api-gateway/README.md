# API Gateway

The API Gateway is the entry point for all client requests in the Leafy microservices architecture. It provides routing, load balancing, circuit breaking, and authentication/authorization.

## Features

- **Dynamic Routing**: Routes requests to appropriate microservices
- **Load Balancing**: Distributes requests across service instances via Eureka
- **Circuit Breaker**: Resilience4j integration for fault tolerance
- **JWT Authentication**: Token validation and user context propagation
- **Token Blacklist**: Redis-based token revocation for logout
- **CORS Support**: Cross-origin resource sharing configuration
- **Request/Response Logging**: Comprehensive logging for debugging
- **Health Checks**: Actuator endpoints for monitoring
- **API Documentation**: SpringDoc OpenAPI integration

## Technology Stack

- Spring Cloud Gateway (WebFlux)
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- Resilience4j Circuit Breaker
- Spring Data Redis Reactive
- JWT (via Common module)
- SpringDoc OpenAPI

## Configuration

### Environment Variables

| Variable                               | Description         | Default                       |
| -------------------------------------- | ------------------- | ----------------------------- |
| `SERVER_PORT`                          | Gateway server port | 8060                          |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka server URL   | http://localhost:8761/eureka/ |
| `SPRING_CLOUD_CONFIG_URI`              | Config server URL   | http://localhost:8888         |
| `SPRING_DATA_REDIS_HOST`               | Redis host          | localhost                     |
| `SPRING_DATA_REDIS_PORT`               | Redis port          | 6379                          |
| `SPRING_DATA_REDIS_PASSWORD`           | Redis password      | redis123                      |
| `JWT_SECRET`                           | JWT signing secret  | (required)                    |

### Service Routes

All routes are prefixed with `/api` and strip this prefix before forwarding:

- **Auth Service**: `/api/auth/**` → `auth-service`
- **User Service**: `/api/users/**` → `user-service`
- **Farm Service**: `/api/farms/**` → `farm-service`
- **File Service**: `/api/files/**` → `file-service`
- **Notification Service**: `/api/notifications/**` → `notification-service`

### Circuit Breaker Configuration

Each service route has a dedicated circuit breaker with:

- Sliding window size: 10 calls
- Minimum calls: 5
- Failure rate threshold: 50%
- Wait duration in open state: 10 seconds
- Request timeout: 5 seconds

## Building

### Local Build

```bash
mvn clean package
```

### Docker Build

```bash
docker build -t leafy-api-gateway:latest .
```

## Running

### Local Run

```bash
# From api-gateway directory
mvn spring-boot:run

# Or run the JAR
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

### Docker Run

```bash
docker run -p 8060:8060 \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-server:8761/eureka/ \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e JWT_SECRET=your-secret-key \
  leafy-api-gateway:latest
```

### Docker Compose

```bash
# From backend directory
docker-compose up api-gateway
```

## API Endpoints

### Health Check

```
GET /actuator/health
```

### Gateway Routes

```
GET /actuator/gateway/routes
```

### Circuit Breaker Status

```
GET /actuator/health
```

## Request Flow

1. Client sends request to API Gateway
2. LoggingFilter logs the incoming request
3. JwtAuthenticationFilter validates JWT token (if required)
4. Token checked against Redis blacklist
5. User context added to request headers
6. Request routed to target service via Eureka
7. Circuit breaker monitors service health
8. Response returned to client

## Security

### Public Endpoints (No Authentication)

- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/refresh`

### Protected Endpoints

All other endpoints require valid JWT token in Authorization header:

```
Authorization: Bearer <jwt-token>
```

### User Context Propagation

Gateway adds these headers to downstream requests:

- `X-Auth-User-Id`: User ID from JWT
- `X-Auth-Username`: Username from JWT

## Redis Token Blacklist

When a user logs out, tokens are blacklisted in Redis:

- Key pattern: `blacklist:token:{token}`
- TTL: Set to remaining token validity

## Monitoring

### Actuator Endpoints

- `/actuator/health` - Health status
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/gateway` - Gateway routes and filters

## Error Handling

### Circuit Breaker Fallbacks

When a service is unavailable, fallback endpoints return:

```json
{
  "status": "error",
  "message": "Service is temporarily unavailable",
  "timestamp": 1234567890
}
```

HTTP Status: 503 Service Unavailable

### Authentication Errors

```json
{
  "status": "error",
  "message": "Invalid JWT token",
  "timestamp": 1234567890
}
```

HTTP Status: 401 Unauthorized

## Development

### Adding a New Route

1. Add route configuration in `application.yaml`:

```yaml
- id: new-service
  uri: lb://new-service
  predicates:
    - Path=/api/new/**
  filters:
    - StripPrefix=1
    - name: CircuitBreaker
      args:
        name: newServiceCircuitBreaker
        fallbackUri: forward:/fallback/new-service
```

2. Add circuit breaker instance in `application.yaml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      newServiceCircuitBreaker:
        baseConfig: default
```

3. Add fallback endpoint in `FallbackController.java`:

```java
@GetMapping("/new-service")
public Mono<ResponseEntity<Map<String, Object>>> newServiceFallback() {
    return Mono.just(createFallbackResponse("New service is temporarily unavailable"));
}
```

## Troubleshooting

### Gateway not routing requests

- Check Eureka registration: `http://localhost:8761`
- Verify service names match route URIs
- Check gateway routes: `/actuator/gateway/routes`

### Circuit breaker always open

- Review failure rate threshold
- Check service health
- Monitor circuit breaker metrics: `/actuator/health`

### JWT validation failing

- Verify JWT_SECRET matches auth-service
- Check token expiration
- Ensure token not blacklisted in Redis

### Redis connection issues

- Verify Redis is running
- Check Redis host/port/password
- Test connection: `redis-cli -h <host> -p <port> -a <password> ping`

## Dependencies

Requires these services to be running:

- Discovery Server (Eureka)
- Config Server (optional)
- Redis (for token blacklist)

## Contributing

1. Follow Spring Cloud Gateway best practices
2. Add circuit breakers for all new routes
3. Implement proper error handling
4. Add logging for debugging
5. Update documentation when adding routes
