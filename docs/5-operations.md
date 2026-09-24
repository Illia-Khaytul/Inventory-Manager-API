# Operations

Application operation design.

## 1. Authentication operations

**Operations:**
1. **Login** : login endpoint
2. **Refresh access** : refresh access endpoint
3. **Logout** : logout endpoint
4. **Logout all** : logout all endpoint

### 1.1. Login

Authenticates the user with the provided credentials and opens a new user session, returning the access and refresh tokens.

**Receives:** `login request`

**Steps:**
1. Authenticate the user with provided credentials. Throws `BadCredentialsException`.
2. Check if user has not reached the maximum open sessions limit. Throws `UserSessionLimitExceededException`.
3. Create new open user session, refresh (UUID) and access (JWT) tokens.
4. Persist the new session and the refresh token.
5. Return access and refresh tokens.

**Returns:** `access token response`
