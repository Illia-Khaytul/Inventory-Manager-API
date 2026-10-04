# Error Handling

Thrown exception definition and their handling.

## 1. Error response format

**ErrorResponse**
- Instant `timestamp`: when did the error happen
- int `status`: what is the returned HTTP status code
- String `message`: what happened
- Map<String, Object> `data`: additional information about the error

Notes:
- The data field is optional and usually empty, but still present to keep the response format consistent for all cases.
It is required for exceptions that have additional information like validation errors and exceptions that require additional data to be truly meaningful like type mismatch or unsupported format.
- Data type is a map of generic objects because the actual data format may vary depending on the exception.
It is less type safe, but doesn't require additional code for special cases.

## 2. Handled exceptions

Exceptions captured and handled by the handlers.

- `UserSessionLimitExceededException`: when user has already opened a maximum number of sessions.
- `FailedLoginAuthenticationException`: when user authentication during login failed.
- `InvalidRefreshTokenException`: when the provided refresh token is not found or used, or its user session is invalid or expired.
- `InvalidPasswordException`: when the provided password does not follow the configured validation rules.
- `DuplicateEntryException`: when an entity with the provided parameter already exists.
- `EntityNotFoundException`: when an entity does not exist.
- `OptimisticLockingFailureException`: when a value is modified concurrently.
- `MethodArgumentNotValidException`: when `@Validated` validation fails. 
- `HandlerMethodValidationException`: when `@Valid` validation fails.
- `MethodArgumentTypeMismatchException`: when the received parameter type does not match the expected type.
- `HttpMessageNotReadableException`: when the request content does not match its content type format.
- `HttpMediaTypeNotSupportedException`: when the request content type is not supported by the endpoint.
- `NoResourceFoundException`: when the request does not point to any exposed endpoint.
- `IllegalStateException`: when something in the application didn't work or isn't configured as intended. 

## 3. Global exception handling

All exceptions not thrown manually (not from the written code) return a custom generic message as to not expose sensitive information in their message on accident. 
Any useful information is manually included in the `data` field.

**UserSessionLimitExceededException handler** -> 403 Forbidden

The user session limit is a business rule. 
Trying to surpass it should return a forbidden operation status code.

**FailedLoginAuthenticationException handler** -> 401 Unauthorized

Used to symbolize any authentication failure during login.
It wraps the actual exception for logging and returns a generic message.

**InvalidRefreshTokenException handler** -> 401 Unauthorized

Returns a generic message for all 4 invalid cases, but still contains the specific messages for logging purposes.

**InvalidPasswordException handler** -> 400 Bad Request

Contains a list of password validation error messages to return to the user as data.

`data` = Password validation errors, mapped as:
- String `errors`: key
- List<String> `errorMessages`: value

**DuplicateEntryException handler** -> 409 Conflict

**EntityNotFoundException handler** -> 404 Not Found

**OptimisticLockingFailureException handler** -> 409 Conflict

`message` = Concurrent modification error

**MethodArgumentNotValidException handler** -> 400 Bad Request

`message` = Invalid request parameters

`data` = Validation errors per field, mapped as:
- String `fieldName`: key
- List<String> `errors`: value

**HandlerMethodValidationException handler** -> 400 Bad Request

`message` = Invalid request parameters

`data` = Validation errors per field, mapped as:
- String `fieldName`: key
- List<String> `errors`: value

**MethodArgumentTypeMismatchException handler** -> 400 Bad Request

`message` = Invalid request parameter type

`data` = Details about the mismatched parameter type:
- String `parameter`: parameter name
- Object `receivedValue`: the received value
- String `requiredType`: the expected parameter type

Notes:
- It is possible that the expected type is null. The method providing it from the exception specifies it as nullable.

**HttpMessageNotReadableException handler** -> 400 Bad Request

`message` = Malformed request body

**HttpMediaTypeNotSupportedException handler** -> 415 Unsupported Media Type

`message` = Unsupported request media type

`data` = The received and supported media types:
- String `received`: received media type name
- List<String> `supported`: supported media types for that request

Notes:
- It is possible for the supported media types to be null. The exception getter specifies it as nullable.

**NoResourceFoundException handler** -> 404 Not Found

`message` = Resource not found

**Generic exception handler** -> 500 Internal Server Error

Catches `IllegalStateException` and any other unexpected exceptions and logs their stack trace.

`message` = Something went wrong

## 4. Security exception handling

All exceptions return a custom message as to not reveal any sensitive information included in the default one.
Additional information is not added to the error response as to not increase complexity by analyzing the internal exception details.

### 4.1. JWT authentication error handler `JwtAuthenticationErrorHandler`

`InvalidBearerTokenException` -> 401 Unauthorized

Invalid or expired access token.

`message` = Invalid or expired access token

`OAuth2AuthenticationException` -> 401 Unauthorized

Malformed or unresolvable access token.

`message` = Access token cannot be resolved

`AuthenticationServiceException` -> 500 Internal Server Error

Unexpected authentication service error.
This exception happens rarely and cannot be reproduced easily, but it is thrown by the OAuth2 bearer token authentication filter.
Due to being unexpected it should be logged and handled appropriately.

`message` = Something went wrong during authentication

### 4.2. General authentication error handler `AuthenticationErrorHandler`

Any `AuthenticationException` -> 401 Unauthorized

`message` = Authentication required to access this resource

### 4.3. Permission error handler `AuthorizationErrorHandler`

Any `AccessDeniedException` -> 403 Forbidden

`message` = Forbidden from accessing this resource