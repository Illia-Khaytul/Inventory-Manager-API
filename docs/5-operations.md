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

### 1.3. Logout

Loads the user session by the provided refresh token, checks if it belongs to the authenticated user and invalidates it.

**Receives:** `refresh token request`

**Steps:**
1. Fetch the refresh token, it's session and user by the provided value. Returns if not found.
2. Check if the user session belongs to the authenticated user. Returns if it doesn't belong.
3. Invalidates the user session.

**Returns:** nothing

**Notes:**
- This logout operation is idempotent.
It always returns the same (nothing) regardless of failure or success.
This keeps the internal workings of the system hidden from outsiders.
- Uses a modifying query to keep the operation atomic.
- Transactional operation because of the modifying query.
The transaction required for the modifying query is located in an inner service method.

### 1.4. Logout all

Invalidates all user sessions that belong to the authenticated user.

**Receives:** nothing

**Steps:**
1. Invalidate all sessions owned by the authenticated user.

**Returns:** nothing

**Notes:**
- This logout operation is idempotent.
  It always returns the same (nothing) whether it closed any sessions or not.
  This keeps the internal workings of the system hidden from outsiders.
- Uses a modifying query to keep the operation atomic.
- Transactional operation because of the modifying query. 
The transaction required for the modifying query is located in an inner service method.

## 2. User operations

**Operations:**
1. **Create user** : create customer and create operator endpoints
2. **Change password** : change password endpoint
3. **Delete user** : delete user endpoint

### 2.1. Create user

Creates a new user with the provided credentials and role.

**Receives:** 
- `create user request`
- UserRole `role`

**Steps:**
1. Validate provided user password. Throws `InvalidPasswordException`.
2. Check if provided username is unique (not taken). Throws `DuplicateEntryException`.
3. Creates new user with provided credentials and role.
4. Return newly created user data.

**Returns:** `user response`

**Notes:**
- User password is encoded before persistence.
- User response does not expose sensitive data (password).
- Operation used by both create customer and create operator endpoints.
Both do the exact same just with different roles and permissions.
- Used by the base operator seeder to create the base OPERATOR user.

### 2.2. Change password

Changes the authenticated user password for the provided new one.

**Receives:** `password change request`

**Steps:**
1. Check if new password is different from old password. Throws `InvalidPasswordException`.
2. Validate provided new password. Throws `InvalidPasswordException`.
3. Load authenticated user. Throws `EntityNotFoundException`.
4. Check if provided old password matches existing new password. Throws `InvalidPasswordException`.
5. Change user password to new one.

**Returns:** nothing

**Notes:**
- Old password is required in the request to validate the password change operation.
- Transactional operation.
It is possible that the authenticated user gets deleted concurrently between being loaded and the password change.
A transaction ensures the user does not get re-inserted into the database after the password change in case that happens.
- Uses the security utility component to load the authenticated user from the database.
Throws `EntityNotFoundException` in case the user got deleted but the access token is still valid.

### 2.3. Delete user

Deletes the currently authenticated user.

**Receives:** nothing

**Steps:**
1. Get authenticated user username.
2. Delete user by username.

**Returns:** nothing

**Notes:**
- Transactional operation because of the modifying delete query.
- Uses a modifying query for user deletion to keep the operation atomic.
- This delete operation is idempotent.
It always returns the same (nothing) regardless of failure or success.
In this case it is to keep the 204 returning operations consistent.


## Base operator seeder

Implements `CommandLineRunner` and executes once on application start.
Is instantiated only when the `spring.application.base_operator.seeder.enable` property is set to `true`.

Populates the database with the base OPERATOR user.

**Receives (on initialization):** 
- String `baseUsername`: required, not blank
- String `basePassword`: required, not blank

**Steps:**
1. Checks if base operator already exists.
If user exists but is not and OPERATOR, throws `IllegalStateException`.
If exists and is OPERATOR, do nothing.
2. Create base OPERATOR user (create user operation).

**Returns:** nothing

**Notes:**
- Throws `IllegalStateException` on initialization if the base operator credentials are null or blank, and when a user with `baseUsername` exists but is not an OPERATOR.
- Uses the create user operation to create the base OPERATOR user.