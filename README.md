# Inventory Manager API

This is a REST API for a simple inventory management system built with Spring Boot implementing JWT security, asynchronous operations and Testcontainers testing.

It showcases:
- Spring Security with JWT authentication and user role based authorization.
- Spring Data JPA with Specifications for filtering and pagination.
- Spring Docker Compose for development database management.
- Flyway for database migrations.
- Transactional operations to ensure atomic value transfer.
- Asynchronous operations for slow processes.
- SpringDoc for API documentation.
- Testcontainers for testing with real database behavior.

## 1. Overview

This application exposes an API to manage products with CRUD operations and manage and monitor the lifecycle of product orders. 
In addition, it performs order lifecycle operations asynchronously to minimize the user response time.

The application implements a JWT authentication system with refresh tokens for access renewal. 
It also separates the users into customer and operator roles for operation authorization.

## 2. Tech Stack

- Spring Boot 4.0.8
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL 18
- Flyway
- Oauth2 Resource Server (for JWT)
- SpringDoc
- Lombok
- MapStruct 1.6.3
- Passay 2.0.0
- Spring Docker Compose
- Docker
- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers

## 3. Prerequisites

- Java 21+
- Maven 3.9.19
- OpenSSL 3
- Docker Engine
- PostgreSQL 18 (for production)

## 4. Architecture

This project follows a monolithic layered architecture with asynchronous processing for background operations.

```text
                .--------------------------------------------------------.
User request ---|--> Controller ----> Service -----------> Repository ---|--> Database
                |                        |                     ^         |
                |                        '-> Async operations -'         |
                '--------------------------------------------------------'
```

## 5. Project Structure

The project follows a domain oriented structure:
- `user` for user account related operations.
- `auth` for user session management operations.
- `order` for order management operations.
- `product` for product management operations.
- `security` for security configuration and setup.
- `exception` for custom exception definition and handling.
- `openapi` for OpenApi configuration and related data.
- `common` for components used in multiple places but not related to one.

## 6. Application Properties

**Max user session limit:** `spring.application.security.user_sessions.limit`

The maximum amount of open sessions a user can have at once.

**User session lifetime:** `spring.application.security.user_sessions.lifetime`

Defines how long can a session remain open in seconds.

Sessions are usually long-lived, so the current value is 60 days (5184000 seconds).

**RSA key sources:** `spring.application.security.jwt.private/public_key_source`

The URIs of the private and public keys used for access token (JWT) encoding/decoding.

Must follow Spring's Resource property format.
In this case the key pair is stored as `.pem` files in the classpath (`classpath:` prefix).

**Access token lifetime:** `spring.application.security.jwt.lifetime`

Defines how long is an access token valid for in seconds.

Access tokens are usually short-lived, so the current value is only 15 minutes (900 seconds).

**Access token issuer:** `spring.application.security.jwt.issuer`

The issuer of the access token (this application).

The current value is `localhost:8080` (local) for development purposes.
It should be changed to the production domain name or server URI for deployment in the [production properties](src/main/resources/application-prod.yaml) file under the same path.

## 7. Getting Started

**Repository:**

First clone the repository to your machine through `https`:

```cmd
git clone https://github.com/Illia-Khaytul/Inventory-Manager-API.git 
```

Or with `ssh`:

```cmd
git clone git clone git@github.com:Illia-Khaytul/Inventory-Manager-API.git
```

**IDE:**

If running the project from an IDE, make sure it has annotation processing enabled.

Go to the official MapStruct [IDE support](https://mapstruct.org/documentation/ide-support/) page for details.

**JWT encoding keys:**

This project uses JWT authentication with a custom RSA key pair. 
The keys are loaded from `.pem` files in the classpath on application startup.
They have to be manually generated as they are excluded from git versioning.

First create a `security/jwt` directory under the [resources](src/main/resources) folder. 
Navigate into it from your terminal.

Generate the private key using OpenSSL:

```cmd
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out jwt_private_key.pem
```

Then extract the public key:

```cmd
openssl rsa -in jwt_private_key.pem -pubout -out jwt_public_key.pem
```

*Note: The property `rsa_keygen_bits:4096` specifies that the private key must be 4096 bits long. 
This is not a requirement and can be changed as necessary. 
Still 4096 is the recommended value.* 

The application loads them by those exact names under that exact path.
Make sure that the `.pem` files are located under `/resources/security/jwt` and are named `jwt_private_key.pem` for the private key and `jwt_public_key.pem` for the public one.

If using this project for personal reasons, the key source locations can be modified in the [properties](src/main/resources/application.yaml) file following Spring's Resource property syntax:

```yaml
spring:
  application:
    security:
      jwt:
        private_key_source: 'classpath:security/jwt/jwt_private_key.pem'
        public_key_source: 'classpath:security/jwt/jwt_public_key.pem'
```

### Run for Development

**Docker:**

This project uses Docker containers to host the development database and Spring Docker Compose to automatically manage its lifecycle.

First create an `.env` file at the project root with the necessary environment variables for the [compose](compose.yaml) file:

```text
DEV_DATABASE_NAME=your_db_name
DEV_DATABASE_USERNAME=your_db_username
DEV_DATABASE_PASSWORD=your_db_password
```

Use this [dev example](.env.dev.example) file as reference.

Make sure your machine does not have environment variables with the same name as these, otherwise docker compose will use them instead.

Then make sure the port used by the database container is not occupied.
Use the command for Windows:

```cmd
netstat -ano | findstr :5435
```

For Linux:

```bash
ss -lntu | grep 5435
```

For MacOS:

```bash
sudo lsof -i :5435
```

The [compose](compose.yaml) file specifies port `5435` as the one to be used by the database container.
Change it to a free one if `5435` is occupied.

Finally check if the Docker Engine is running:

```cmd
docker ps
```

This command shows a list of containers and their status and details.
If the engine is not running it will show an ERROR message.

Spring Docker Compose then automatically creates and manages the lifecycle of the containers defined in the [compose](compose.yaml) file.

**Run the application:**

Now start the application:

```cmd
mvn spring-boot:run
```

Or run with your IDE.

The default profile is `dev`, so no further configuration is necessary.

The application will be available at `http://localhost:8080/api/v1` after initialization.

### Run for Production

This project does not use Docker to host the production database, it has to be configured and activated separately.

**Environment variables:**

Add the required environment variables used to connect to the database:

```text
PROD_DATABASE_URL
PROD_DATABASE_USERNAME
PROD_DATABASE_PASSWORD
```

Use this [prod example](.env.prod.example) as reference for the right names and content format.

Restart the IDE/terminal so it picks up the new variables.

**Run the application:**

Change the active profile to `prod` in the [properties](src/main/resources/application.yaml) file:

```yaml
spring:
  profiles:
    active: prod
```

Or use this CLI argument when starting the application:

```cmd
-Dspring-boot.run.profiles=prod
```

Now you can run the application:

```cmd
mvn spring-boot:run "-Dspring-boot.run.profiles=prod"
```

Surround the property with `"` so maven doesn't read it as a lifecycle stage.

The application will be available at `http://localhost:8080/api/v1` after initialization.

## 8. API Documentation

This project uses SpringDoc to automatically generate a documentation for the API based on the exposed endpoints.

The documentation is available at `http://localhost:8080/api/v1/swagger-ui/index.html` for the `dev` profile.

## 9. Security

The application uses JWT for authentication and rotating, single use, opaque refresh tokens for JWT renewal.
All endpoints except user registration, login, access refresh and product viewing will require authentication.

Additionally, all users will be split by customer and operator roles.
Operators will be able to manage products and view all orders, but not create their own orders.
Customers will be able to view products and create and track their orders, but not manage products.

Passwords and refresh tokens are encoded before being persisted.

## 10. Run Tests

Includes unit tests for individual components, slice tests for the web and database layer (controllers and repositories), integration tests for component coordination, and end-to-end tests for full application workflow.

Utilizes Testcontainers for tests against a real database.

To run the tests make sure Docker Engine is running.
Use the `docker ps` command for that. Then run all the tests:

```cmd
mvn verify
```

Or just the unit and slice tests:

```cmd
mvn test
```

## 11. Design Documentation

The design documentation is located at the [docs](docs/1-overview.md) folder.