# Entities

Entity design and definition.

Uses Flyway for table and relationship creation.

## 1. User

Stores the user credentials and role.

**Fields:**
- Long `id`: primary key, identity
- String `username`: required, unique
- String `password`: required
- UserRole `role`: required, saved as String

Notes:
- Username is unique.
- Password will be hashed.

## 2. UserSession

Stores the session expiry and whether it's valid or not. Defines an opened or closed user session.

**Fields:**
- Long `id`: primary key, identity
- boolean `valid`: required
- Instant `createdAt`: required
- Instant `expiresAt`: required
- User `user`: foreign key for User, LAZY fetching, required, on delete cascade

Notes:
- User sessions are persisted after being closed for logging purposes.
- Delete all session records with the user.
User sessions are part of a user's auth history. 
Deleting a user means there is no need to keep the records.

## 3. RefreshToken

Stores the refresh token value and its data.

**Fields:**
- Long `id`: primary key, identity
- String `tokenValue`: required, unique
- Instant `issuedAt`: required
- boolean `used`: required
- UserSession `user_session`: foreign key for UserSession, EAGER fetching, required, on delete cascade 
- Integer `version`: version

Notes:
- Token value will be hashed.
- Refresh tokens will be persisted along their related user sessions for logging purposes.
- Delete all tokens with their related session.
Refresh tokens are tightly related to user sessions.
There cannot be a refresh token that does not point to a session.
- Versioning to ensure the token gets used (used flag flipped) only once.