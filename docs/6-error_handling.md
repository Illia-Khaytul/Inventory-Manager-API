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
- `MethodArgumentNotValidException`: when `@Validated` validation fails. 
- `HandlerMethodValidationException`: when `@Valid` validation fails.
- `MethodArgumentTypeMismatchException`: when the received parameter type does not match the expected type.
- `HttpMessageNotReadableException`: when the request content does not match its content type format.
- `HttpMediaTypeNotSupportedException`: when the request content type is not supported by the endpoint.
- `NoResourceFoundException`: when the request does not point to any exposed endpoint.

## 3. Global exception handling

All exceptions not thrown manually (not from the written code) return a custom generic message as to not expose sensitive information in their message on accident. 
Any useful information is manually included in the `data` field.

**UserSessionLimitExceededException handler** -> 403 Forbidden

The user session limit is a business rule. 
Trying to surpass it should return a forbidden operation status code.

**FailedLoginAuthenticationException handler** -> 401 Unauthorized

Used to symbolize any authentication failure during login.
It wraps the actual exception for logging and returns a generic message.

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

Catches any other unexpected exceptions and logs their stack trace.

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