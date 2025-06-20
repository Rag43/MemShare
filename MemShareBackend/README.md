# MemShare Backend

A Spring Boot application for memory sharing with JWT authentication, PostgreSQL database, and AWS S3 integration.

## 🚀 Features

- **JWT Authentication**: Secure user authentication and authorization
- **PostgreSQL Database**: Reliable data persistence with Flyway migrations
- **AWS S3 Integration**: File storage for media uploads
- **Memory Groups**: Collaborative memory sharing functionality
- **RESTful API**: Clean and documented API endpoints
- **Spring Security**: Comprehensive security implementation

## 📋 Prerequisites

- Java 23 or higher
- Maven 3.6+
- PostgreSQL 12+
- AWS Account (for S3 functionality)

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/MemShare.git
cd MemShare/MemShareBackend
```

### 2. Database Setup
```sql
-- Create database
CREATE DATABASE jwt_security;
CREATE DATABASE jwt_security_dev;

-- Create user (if not exists)
CREATE USER raghav WITH PASSWORD 'Lincoln438917';
GRANT ALL PRIVILEGES ON DATABASE jwt_security TO raghav;
GRANT ALL PRIVILEGES ON DATABASE jwt_security_dev TO raghav;
```

### 3. Environment Configuration

#### Development Environment
```bash
# Set environment variables for development
export SPRING_PROFILES_ACTIVE=dev
```

#### Production Environment
```bash
# Set required environment variables
export DATABASE_URL=jdbc:postgresql://your-db-host:5432/jwt_security
export DATABASE_USERNAME=your_db_user
export DATABASE_PASSWORD=your_db_password
export JWT_SECRET=your-256-bit-secret-key
export AWS_ACCESS_KEY_ID=your_aws_access_key
export AWS_SECRET_ACCESS_KEY=your_aws_secret_key
export S3_BUCKET_NAME=your-s3-bucket-name
export SPRING_PROFILES_ACTIVE=prod
```

### 4. Build and Run

#### Using Maven
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run the application
mvn spring-boot:run
```

#### Using Maven Wrapper
```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Run the application
./mvnw spring-boot:run
```

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/rag/JwtLearn/
│   │   ├── auth/           # Authentication & JWT
│   │   ├── config/         # Configuration classes
│   │   ├── media/          # Media/S3 handling
│   │   ├── memory/         # Memory management
│   │   ├── memoryGroup/    # Memory groups
│   │   └── user/           # User management
│   └── resources/
│       ├── application.properties
│       ├── application-dev.properties
│       ├── application-prod.properties
│       └── db/migration/   # Flyway migrations
└── test/                   # Test classes
```

## 🔧 Configuration

### Application Properties

| Property | Description | Default |
|----------|-------------|---------|
| `spring.datasource.url` | Database connection URL | `jdbc:postgresql://localhost:5432/jwt_security` |
| `jwt.secret` | JWT signing secret | `your-256-bit-secret-key-here-change-in-production` |
| `jwt.expiration` | JWT token expiration (ms) | `86400000` (24 hours) |
| `aws.s3.bucket-name` | S3 bucket name | `your-s3-bucket-name` |
| `server.port` | Application port | `8080` |

### Environment Profiles

- **dev**: Development configuration with detailed logging
- **prod**: Production configuration with environment variables

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MemoryGroupServiceTest

# Run with coverage
mvn test jacoco:report
```

## 📚 API Documentation

The application exposes RESTful endpoints for:

- **Authentication**: `/api/v1/auth/register`, `/api/v1/auth/authenticate`
- **Memories**: `/api/v1/memories/**`
- **Memory Groups**: `/api/v1/groups/**`
- **Media**: `/api/v1/media/**`

### Example API Usage

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstname":"John","lastname":"Doe","email":"john@example.com","password":"password123"}'

# Authenticate user
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"password123"}'
```

## 🔒 Security

- JWT-based authentication
- Password encryption with BCrypt
- Role-based access control
- CORS configuration for frontend integration
- Input validation and sanitization

## 🚀 Deployment

### Docker Deployment
```bash
# Build Docker image
docker build -t memshare-backend .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=your_db_url \
  memshare-backend
```

### Cloud Deployment
The application is configured for deployment on:
- AWS Elastic Beanstalk
- Heroku
- Google Cloud Run
- Azure App Service

## 📝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Support

For support and questions:
- Create an issue in the GitHub repository
- Contact: raghav@example.com

## 🔄 Changelog

### Version 0.0.1-SNAPSHOT
- Initial release
- JWT authentication implementation
- Memory and memory group functionality
- AWS S3 integration
- PostgreSQL database with Flyway migrations 