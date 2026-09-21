# API Contract

Design and definition of the API contract and its request and response data.

Base endpoint path is `/api/v1`.

## 1. Authentication API

**Endpoints:**
1. **Login** : user credentials authentication, opens a new session, returns a JWT access token and a refresh token
2. **Refresh access** : uses up the refresh token, returns a new JWT and refresh token
3. **Logout** : idempotent logout, closes the session by refresh token
4. **Logout all** : idempotent logout, closes all open sessions for user

**Request DTOs:**

`login request`:
- String `username`: required, not blank, max length 50
- String `password`: required, not blank, max length 50

`refresh token request`:
- String `refreshToken`: required, not blank, max length 50

**Response DTOs:**

`access token response`:
- String `issuer`
- Instant `issuedAt`
- Instant `expiresAt`
- String `subject`
- String `role`
- String `accessToken`
- String `refreshToken`

### 1.1. Login

**POST** `/auth/login`

Does not require authentication.

**Receives:**
- Body: `login request`

**Returns:**
- Body: `access token response`

**Responses:**
- 200 OK: user authenticated and new session opened
- 400 Bad Request: request validation failed
- 401 Unauthorized: failed to authenticate with provided credentials
- 403 Forbidden: exceeds max open session limit

### 1.2. Refresh access

**POST** `/auth/refresh-access`

Does not require authentication.

**Receives:**
- Body: `refresh token request`

**Returns:**
- Body: `access token response`

**Error Responses:**
- 200 OK: refresh token valid and new access token generated
- 400 Bad Request: request validation failed
- 401 Unauthorized: used, invalid or expired refresh token (can't use this token to gain access)

### 1.3. Logout

**POST** `/auth/logout`

Requires authentication.

**Receives:**
- Body: `refresh token request`

**Returns:** nothing

**Error Responses:**
- 204 No Content: session closed by refresh token
- 400 Bad Request: request validation failed
- 401 Unauthorized: not authenticated

### 1.4. Logout all

**POST** `/auth/logout-all`

Requires authentication.

**Receives:** nothing

**Returns:** nothing

**Error Responses:**
- 204 No Content: all open sessions closed
- 401 Unauthorized: not authenticated