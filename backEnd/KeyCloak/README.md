# Keycloak Auth Microservice

Authentication microservice for the **Smart Freelance and Project Matching Platform**. It uses **Keycloak** as the identity provider and exposes REST APIs for registration, token (login), userinfo, and token validation. The **User service** (and other microservices) can rely on this service for authentication and JWT validation.

## Features

- **Register** – Create users in Keycloak with roles: `CLIENT`, `FREELANCER`, `ADMIN` (aligned with the User service).
- **Token** – Password grant: exchange username/password for JWT access and refresh tokens.
- **Userinfo** – Return current user info from the JWT (for downstream services).
- **Validate** – Validate a Bearer JWT and return subject/expiry (for service-to-service auth).

All protected endpoints expect a valid Keycloak JWT in the `Authorization: Bearer <token>` header.

## Prerequisites

- **Java 17**
- **Keycloak** running locally (see below; no Docker required)
- **Eureka** running (e.g. `http://localhost:8420`) so this service can register

## Running Keycloak Locally (No Docker)

1. **Download Keycloak**
   - Go to [https://www.keycloak.org/downloads](https://www.keycloak.org/downloads) and download the **Keycloak 25** (or current) distribution for your OS (e.g. ZIP for Windows).

2. **Extract and start**
   - Extract the archive and open a terminal in the Keycloak root directory.
   - **Windows (PowerShell):**
     ```powershell
     .\bin\kc.bat start-dev
     ```
   - **Linux / macOS:**
     ```bash
     ./bin/kc.sh start-dev
     ```
   - By default Keycloak listens on **http://localhost:8080**. The auth service is preconfigured for this. If you use a different port (e.g. 8180), set `keycloak.auth-server-url` in `application.properties` accordingly.

3. **Create admin user**
   - On first start you will be prompted to create an admin user (e.g. username: `admin`, password: `admin`). Use these in `application.properties` for `keycloak.admin.username` and `keycloak.admin.password`.

4. **Create realm and client**
   - Open **http://localhost:8080** (or your Keycloak URL) and log in to the Admin Console.
   - **Realm:** Create a new realm named **`smart-freelance`** (or change `keycloak.realm` in config).
   - **Realm roles:** In **Realm roles**, create three roles: **`CLIENT`**, **`FREELANCER`**, **`ADMIN`** (must match the User service enum).
   - **Client:** In **Clients**, create a client:
     - **Client ID:** `smart-freelance-backend`
     - **Client authentication:** ON (confidential client).
     - **Valid redirect URIs:** add your frontend/app URLs (e.g. `http://localhost:3000/*`).
     - **Direct access grants:** ON (required for password grant used by `/api/auth/token`).
     - Save and copy the **Client secret** from the **Credentials** tab into `application.properties` as `keycloak.credentials.secret` (or set env `KEYCLOAK_CLIENT_SECRET`).

5. **Optional:** If Keycloak runs on another port (e.g. 8180), set:
   ```properties
   keycloak.auth-server-url=http://localhost:8180
   ```

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | Auth service port | `8081` |
| `keycloak.auth-server-url` | Keycloak server URL | `http://localhost:8080` |
| `keycloak.realm` | Realm name | `smart-freelance` |
| `keycloak.resource` | Client ID | `smart-freelance-backend` |
| `keycloak.credentials.secret` | Client secret (or `KEYCLOAK_CLIENT_SECRET`) | - |
| `keycloak.admin.username` | Admin console username | `admin` |
| `keycloak.admin.password` | Admin console password (or `KEYCLOAK_ADMIN_PASSWORD`) | `admin` |
| `eureka.client.service-url.defaultZone` | Eureka URL | `http://localhost:8420/eureka` |

## Running the Auth Service

1. Start **Eureka** (e.g. on port 8420).
2. Start **Keycloak** and complete realm/client setup as above.
3. From the project root:
   ```bash
   ./mvnw spring-boot:run
   ```
   Or with Maven:
   ```bash
   mvn spring-boot:run
   ```
4. Service runs at **http://localhost:8081**.
5. Swagger UI: **http://localhost:8081/swagger-ui.html**

## API Overview

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register a new user in Keycloak (body: email, password, firstName, lastName, role). |
| POST | `/api/auth/token` | No | Get access/refresh tokens (body: username, password). |
| GET | `/api/auth/userinfo` | Bearer JWT | Current user info from JWT. |
| GET | `/api/auth/validate` | Bearer JWT | Validate token; returns `valid`, `sub`, `exp`. |

## Integration with User Service

- **Registration flow:** Frontend or gateway can call this service’s **POST /api/auth/register**, then call the **User service** to create the local user profile (with the same email and role). Alternatively, the User service can call this auth service to create the user in Keycloak first, then create the local user.
- **Login:** Use **POST /api/auth/token** with email/password to get a JWT. Use the JWT in `Authorization: Bearer <token>` for this service and for the User service (if the User service is configured as an OAuth2 resource server with the same Keycloak issuer).
- **Validation:** Other microservices can call **GET /api/auth/validate** with the same JWT to confirm the token is valid, or they can validate the JWT themselves using Keycloak’s JWKS URI (same issuer as this service).

## Docker

Docker support can be added later; this README focuses on running Keycloak and the auth service without Docker.
