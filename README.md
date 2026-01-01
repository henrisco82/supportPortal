# Support Portal Application

A comprehensive user management and authentication system built with Spring Boot 2.7.18 and Java 17. This application provides a complete backend solution for user registration, authentication, profile management, and administrative functions.

## 🚀 Features

- **JWT Authentication**: Secure token-based authentication system
- **User Management**: Complete CRUD operations for user accounts
- **Role-Based Access Control**: Admin and User roles with different permissions
- **Profile Management**: User profile updates and profile image uploads
- **Email Integration**: Password reset functionality via email
- **RESTful API**: Well-documented REST endpoints
- **Database Support**: H2 for testing, MySQL for production
- **API Documentation**: Swagger/OpenAPI 3.0 documentation
- **Security**: Spring Security with custom filters and handlers

## 🛠️ Technology Stack

- **Java**: 17
- **Spring Boot**: 2.7.18
- **Spring Security**: JWT-based authentication
- **Spring Data JPA**: Database operations
- **Database**: H2 (development), PostgreSQL (production)
- **Profiles**: Spring profiles for environment switching
- **Email**: JavaMail API
- **Documentation**: SpringDoc OpenAPI (Swagger)
- **Containerization**: Docker support
- **Deployment**: Render cloud platform
- **Build Tool**: Maven
- **Other Libraries**:
  - Auth0 JWT: Token generation and validation
  - Apache Commons Lang3: Utility functions
  - ModelMapper: Object mapping
  - Lombok: Code generation
  - Guava: Additional utilities

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL (for production) or H2 (included for development)

## 🔧 Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd supportPortal
```

### 2. Quick Setup (Recommended)
```bash
# Run the automated setup script
./setup.sh
```

This script will:
- Create your `.env` file from the template
- Build the application
- Provide helpful next steps

### 3. Default Super Admin Account
When you first start the application, a **Super Admin** user is automatically created:

- **Username:** `supportPortal`
- **Password:** `supportPortal`
- **Role:** SUPER_ADMIN (full access to all features)
- **Email:** `admin@supportportal.com`

**⚠️ Security Notice:** Change the default password after first login!

### 4. CORS Configuration
✅ **CORS is pre-configured** to allow your Angular frontend on `localhost:4200`

### 2. Manual Setup
If you prefer manual setup:

```bash
# Copy environment template
cp env.example .env

# Edit with your values
nano .env

# Build the application
./mvnw clean install
```

### 3. Run the Application
```bash
# Run with H2 database (default for local development)
./mvnw spring-boot:run

# Or run with PostgreSQL profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

The application will start on `http://localhost:8081`

### 4. Environment Variables
Configure these environment variables for customization:

#### Application Configuration
```bash
# Application
export PORT=8081
export SPRING_PROFILES_ACTIVE=h2  # or 'postgres'

# JWT
export JWT_SECRET=your_secure_jwt_secret_here

# Email (for password reset functionality)
export EMAIL_USERNAME=your-email@gmail.com
export EMAIL_PASSWORD=your-app-password
```

#### Docker Compose Environment File
For Docker Compose development, create a `.env` file from the template:

```bash
cp env.example .env
# Edit .env with your values
```

The `.env` file is gitignored and should never be committed to version control.

#### Production Environment Variables
When deploying to production platforms like Render, set these environment variables:

- `SPRING_PROFILES_ACTIVE=postgres`
- `DATABASE_URL` - Your production database URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `JWT_SECRET` - Secure JWT signing secret
- `EMAIL_USERNAME` - SMTP username (optional)
- `EMAIL_PASSWORD` - SMTP password (optional)

## ⚙️ Configuration

The application uses Spring profiles to switch between different environments:

### Default Profile
- **Default**: `h2` (local testing)
- **Production**: `postgres`

### Development (H2 Database)
The application defaults to H2 for local development:
```bash
# Run with H2 (default)
./mvnw spring-boot:run

# Or explicitly specify
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

H2 Console access:
- **URL**: `http://localhost:8081/h2-console`
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (empty)

### Production (PostgreSQL Database)
For production with PostgreSQL:
```bash
# Run with PostgreSQL profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Environment variables for PostgreSQL:
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/supportportal
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret
```

## 📚 API Documentation

### Swagger UI
Access the interactive API documentation at:
```
http://localhost:8081/swagger-ui/index.html
```

### OpenAPI Specification
The API specification is available at:
```
http://localhost:8081/v3/api-docs
```

## 🔐 Authentication

The application uses JWT (JSON Web Tokens) for authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## 📡 API Endpoints

### Authentication
- `POST /user/login` - User login
- `POST /user/register` - User registration

### User Management
- `POST /user/add` - Add new user (Admin)
- `POST /user/update` - Update user information
- `GET /user/find/{username}` - Get user by username
- `GET /user/list` - Get all users
- `DELETE /user/delete/{id}` - Delete user (Admin)

### Profile Management
- `POST /user/updateProfileImage` - Update profile image
- `GET /user/image/{username}/{filename}` - Get profile image
- `GET /user/image/profile/{username}` - Get temporary profile image

### Password Management
- `GET /user/resetPassword/{email}` - Reset password via email

## 📨 API Data Format

The authentication endpoints use **dedicated DTOs** for clean API contracts:

### Login Request (`LoginRequest`)
```json
POST /user/login
Content-Type: application/json

{
  "username": "your_username",
  "password": "your_password"
}
```

### Register Request (`RegisterRequest`)
```json
POST /user/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe",
  "email": "john@example.com"
}
```

**Note:** The API uses dedicated DTOs (`LoginRequest`, `RegisterRequest`) instead of the full `User` entity for better API design and documentation.

### 📋 DTOs Overview

- **`LoginRequest`**: Contains only `username` and `password` fields for authentication
- **`RegisterRequest`**: Contains `firstName`, `lastName`, `username`, and `email` for user registration
- **Validation**: Both DTOs include validation annotations for required fields
- **Swagger**: API documentation shows clean, focused request examples

### 🌐 CORS Configuration

The application is configured to allow cross-origin requests from:

- **Development**: `http://localhost:4200` (Angular CLI default)
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS, PATCH
- **Credentials**: Enabled (for authentication cookies/tokens)
- **Max Age**: 3600 seconds (1 hour)

**Frontend Integration**: Your Angular app on port 4200 can now communicate with this API without CORS errors.

### Response
Both endpoints return the user object with JWT token in the response headers:
```json
{
  "id": 1,
  "userId": "uuid",
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe",
  "email": "john@example.com",
  // ... other user fields
}
```

**Headers:**
```
Jwt-Token: Bearer <jwt_token>
```

## 👥 User Roles

### ADMIN
- Full access to all user management operations
- Can create, update, and delete users
- Can manage user roles and status

### USER
- Can update own profile
- Can manage own profile image
- Limited access to user data

## 🗂️ Project Structure

```
src/main/java/com/supportportal/
├── configuration/          # Configuration classes
│   ├── OpenApiConfig.java
│   ├── SecurityConfiguration.java
│   └── WebConfig.java              # CORS & resource configuration
├── constant/               # Application constants
├── domain/                 # Entity classes & DTOs
│   ├── User.java
│   ├── UserPrincipal.java
│   ├── HttpResponse.java
│   ├── LoginRequest.java       # Login DTO
│   └── RegisterRequest.java    # Registration DTO
├── enumeration/            # Enums
│   └── Role.java
├── exception/              # Custom exceptions
├── filter/                 # Security filters
├── listener/               # Authentication listeners
├── repository/             # Data repositories
├── resource/               # REST controllers
├── service/                # Business logic
└── utility/                # Utility classes
    └── DatabaseSeeder.java   # Auto-creates super admin user
```

## 🧪 Testing

Run the tests using Maven:
```bash
./mvnw test
```

## 🚀 Deployment

### Local Development with Docker Compose
For local testing with PostgreSQL:

```bash
# 1. Copy the environment template
cp env.example .env

# 2. Edit .env file with your own values
nano .env  # or your preferred editor

# 3. Build and run with Docker Compose
docker-compose up --build
```

**Note:** The `.env` file contains sensitive information and is gitignored. Never commit it to the repository.

### JAR File
Build a production JAR:
```bash
./mvnw clean package -DskipTests
```

Run the JAR:
```bash
# With H2 (default)
java -jar target/SupportPortalApp-0.0.1-SNAPSHOT.jar

# With PostgreSQL
java -jar target/SupportPortalApp-0.0.1-SNAPSHOT.jar --spring.profiles.active=postgres
```

### Docker Deployment
Build and run with Docker:
```bash
# Build the image
docker build -t support-portal .

# Run with H2
docker run -p 8081:8081 support-portal

# Run with PostgreSQL (set environment variables)
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=postgres \
  -e DATABASE_URL=jdbc:postgresql://host:5432/supportportal \
  -e DB_USERNAME=user \
  -e DB_PASSWORD=password \
  -e JWT_SECRET=your_secret \
  support-portal
```

### Render Deployment
Deploy to Render using the provided configuration:

1. **Connect Repository**: Link your GitHub repository to Render
2. **Auto-deployment**: Render will automatically build and deploy using `render.yaml`
3. **Database**: PostgreSQL database will be automatically provisioned
4. **Environment Variables**: Configure additional secrets as needed

The `render.yaml` file includes:
- Web service configuration with Docker
- PostgreSQL database setup
- Environment variables configuration
- Health check endpoint
- Persistent disk for file uploads

## 🔒 Security Features

- **JWT Authentication**: Stateless authentication with secure tokens
- **Password Encryption**: BCrypt password hashing
- **CORS Configuration**: Cross-origin resource sharing setup
- **Request Filtering**: Custom security filters
- **Login Attempt Tracking**: Prevent brute force attacks
- **Role-based Authorization**: Method-level security
- **Environment Variables**: Sensitive data stored in environment variables, not in code
- **Gitignored Secrets**: `.env` files are excluded from version control

### 🔐 Security Best Practices

- **Never commit secrets**: Use environment variables for sensitive data
- **Use strong passwords**: Generate secure passwords for databases and JWT secrets
- **Environment isolation**: Different secrets for development, staging, and production
- **Regular rotation**: Rotate secrets periodically
- **Access control**: Limit access to environment variables in production

## 📧 Email Configuration

For email functionality (password reset), configure the following environment variables:

```bash
# Required for email functionality
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password
```

Or configure in `application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support and questions, please contact the development team at support@supportportal.com

## 🗺️ Roadmap

- [ ] Multi-factor authentication
- [ ] OAuth2 integration
- [ ] Audit logging
- [ ] User activity tracking
- [ ] Advanced search and filtering
- [ ] API rate limiting
- [ ] User notification system
