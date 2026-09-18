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

- `MethodArgumentNotValidException`: when `@Validated` validation fails. 
- `HandlerMethodValidationException`: when `@Valid` validation fails.
- `MethodArgumentTypeMismatchException`: when the received parameter type does not match the expected type.
- `HttpMessageNotReadableException`: when the request content does not match its content type format.
- `HttpMediaTypeNotSupportedException`: when the request content type is not supported by the endpoint.
- `NoResourceFoundException`: when the request does not point to any exposed endpoint.

## 3. Global exception handling

All exceptions not thrown manually (not from the written code) return a custom generic message as to not expose sensitive information in their message on accident. 
Any useful information is manually included in the `data` field.

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
