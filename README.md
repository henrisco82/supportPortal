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
- **Database**: H2 (development), MySQL (production)
- **Email**: JavaMail API
- **Documentation**: SpringDoc OpenAPI (Swagger)
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

### 2. Build the Application
```bash
./mvnw clean install
```

### 3. Run the Application
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8081`

## ⚙️ Configuration

### Development (H2 Database)
The application is pre-configured to use H2 in-memory database for development:
- **URL**: `http://localhost:8081/h2-console`
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (empty)

### Production (MySQL Database)
For production, update `application.properties`:
```properties
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/supportportal?createDatabaseIfNotExist=true
spring.datasource.username=your_username
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
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
│   └── WebConfig.java
├── constant/               # Application constants
├── domain/                 # Entity classes
│   ├── User.java
│   ├── UserPrincipal.java
│   └── HttpResponse.java
├── enumeration/            # Enums
│   └── Role.java
├── exception/              # Custom exceptions
├── filter/                 # Security filters
├── listener/               # Authentication listeners
├── repository/             # Data repositories
├── resource/               # REST controllers
├── service/                # Business logic
└── utility/                # Utility classes
```

## 🧪 Testing

Run the tests using Maven:
```bash
./mvnw test
```

## 🚀 Deployment

### JAR File
Build a production JAR:
```bash
./mvnw clean package -DskipTests
```

Run the JAR:
```bash
java -jar target/SupportPortalApp-0.0.1-SNAPSHOT.jar
```

### Docker (Optional)
Create a Dockerfile for containerization:

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 🔒 Security Features

- **JWT Authentication**: Stateless authentication with secure tokens
- **Password Encryption**: BCrypt password hashing
- **CORS Configuration**: Cross-origin resource sharing setup
- **Request Filtering**: Custom security filters
- **Login Attempt Tracking**: Prevent brute force attacks
- **Role-based Authorization**: Method-level security

## 📧 Email Configuration

For email functionality, configure the following properties in `application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
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
