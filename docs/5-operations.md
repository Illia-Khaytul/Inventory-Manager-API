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
1. Authenticate the user with provided credentials. Throws `FailedLoginAuthenticationException`.
2. Check if user has not reached the maximum open sessions limit. Throws `UserSessionLimitExceededException`.
3. Create new open user session, refresh (UUID) and access (JWT) tokens.
4. Persist the new session and the refresh token.
5. Return access and refresh tokens.

**Returns:** `access token response`

**Notes:**
- Transactional operation.
It is rare, but two different refresh token UUIDs may collide which will throw an exception.
In that case it is needed to roll back all the performed operations.

### 1.2. Refresh access

Checks for provided refresh token reuse or session invalidation and marks it as used, then generates a new one for the same session and returns it with a new access token.

**Receives:** `refresh token request`

**Steps:**
1. Fetch the refresh token, it's session and user by the provided value. Throws `InvalidRefreshTokenException`.
2. Check if refresh token is not used and the session not invalid or expired.
If not the case marks the session as invalid and throws `InvalidRefreshTokenException`.
3. Mark refresh token as used.
4. Create new refresh token for the same session and new access token.
5. Persist the new refresh token.
6. Return new access and refresh tokens.

**Returns:** `access token response`

**Notes:**
- Transactional operation. If the token has been used concurrently all the changes must be rolled back.
- Session invalidation requires a separate transaction.
If the token was used, or the session invalid or expired an exception is thrown which rolls back the main operation transaction.
Session invalidation must be committed regardless of that.
- Session invalidation is performed with a modifying query for atomic execution.
- Right now the user receives a 409 Conflict response if the refresh token was being used concurrently.
When that happens it means that the token has probably been leaked and therefore the session should be closed.
In any case that will always happen if the user tries resending the same request after receiving the error response.
This functionality has not been implemented because it is outside the scope of this project, is extremely rare to occur naturally and the current token validity check already covers most of the invalidation cases.
- `InvalidRefreshTokenException` receives a generic message for all invalid refresh token cases for the user response, but the specific messages are still collected for loggin purposes. 