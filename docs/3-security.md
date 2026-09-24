# Security

Security configuration and component definition.

## 1. Main authentication

This application uses JWT access tokens for user authentication.
It loads an RSA key pair from `.pem` files for JWT encoding and decoding.
The encoders/decoders use an `RS512` algorithm for signature generation.
Furthermore, user roles are extracted from a `roles` claim rather than the usual `scopes`.

### 1.1. Configuration

- by default all requests require authentication
- `browser sessions` disabled (stateless)
- `csrf` disabled
- `form login` disabled
- `http basic` disabled
- `oauth2 resource server` configured to use JWT authentication

### 1.2. Error handling
Error handling is divided into 3 parts. 
JWT authentication, general authentication and permission (authorization) error handling.

JWT authentication error handling is configured for cases such as invalid access token, malformed access token and unexpected service errors.
It is added to the OAuth2 resource server.

General authentication error handling is for cases when the user is unauthenticated (accesses without an access token).
It is added to the filter chain's error handling.

Permission (authorization) error handling is for cases when the user is not allowed to access a certain resource.
It is also added to the filter chain's error handling.

Details on the exact responses is provided in the [error handling](6-error_handling.md) docs.