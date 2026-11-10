# Additional Elements

Additional non-service functional component design.

## Security utils

Enables easy access to user authentication data like the authenticated username or role.
Does not require the authentication to be strictly JWT, which makes adding new authentication methods easier and allows the use of `TestingAuthenticationToken` for testing.

This utility is supposed to be used in operations requiring authentication.
For this reason invalid authentication, authenticated username or role signals misconfiguration and should return 500 Internal Server Error.

**Get authentication token:**

Gets the authentication token from the `SecurityContextHolder`.
Throws `IllegalStateException` if the authentication token is not present or present but not authenticated.

**Get authenticated username:**

Gets the authenticated username (principal name) from the current authentication.
Calls the `get authentication token` method to get it.
Throws `IllegalStateException` if the username is null or blank.

**Get authenticated user role:**

Extracts the authenticated user role name from the current authentication granted authorities.
Only counts as a user role authorities that begin with `ROLE_` (default Spring Security role prefix).
Calls the `get authentication token` method to get the authentication token.
Throws `IllegalStateException` if there are zero or more than one user roles present for the authenticated user.

**Load authenticated user:**

Loads the user entity by authenticated username.
Calls the `get authenticated username` method to get it.
Throws `EntityNotFoundException` if the authenticated user does not exist.
This may happen if the user has been deleted but the access token (JWT) remains valid.
This is not an application error and therefore should not return a 500 Internal Server Error response.

## Entity auditing

Entities that have auditing fields use Spring's provided entity auditing.
A custom `AuditorAware` implementation is used for created/modified by fields.
It uses the `security utils` to get the authenticated username.

## Decimals

Provides methods for consistent `BigDecimal` operations across the application.
Always returns `BigDecimal` with a scale of 2 and half up rounding following the application price value format.

## Response view resolvers

Custom implementations of `ResponseBodyAdvice` that determine the JSON view applicable to a response.

**Role based view resolver:**

Determines a JSON view based on the currently authenticated user role.
Applies only to endpoints annotated with the `RoleBasedJsonView` annotation.
It uses the `security utils` to get the authenticated user role.

It applies the `OperatorView` view if the authenticated user is an OPERATOR and the `DefaultView` in any other case.
Requires Jackson's default view inclusion to be set to true in order to work properly.

## Special serializers and deserializers

**Monetary JSON serializer:**

Serializes `BigDecimal` values to HTTP responses following the application price value format.
Uses the `decimals` utility component.

**Monetary JSON deserializer:**

Deserializes `BigDecimal` values from HTTP requests following the application price value format.
Uses the `decimals` utility component.

**Snake case serializer:**

Manually converts camel case String values to snake case.
It is used for field names that do not automatically get converted by Jackson (like map key values).

## Validation components

Custom `ConstraintValidator` implementations and utility components used for special request validations.

**Validation properties:**

Holds all the validation limits and values used by property based validators.
It exposes getter methods to access them via the `PropertyKey` enum.

**Not zero validator:**

Validates that a numeric value is either null or not zero.

**Null or not blank validator:**

Validates that a String value is either null or contains at least one non whitespace character.

**Range property validator:**

Validates that a `BigDecimal` or numeric value is within a certain range.
The range limit is stored in the `validation properties` component, but it can be overridden in the `RangeProperty` annotation. 

**Size property validator:**

Validates that a String, collection or map size is within a certain range.
The range limit is stored in the `validation properties` component, but it can be overridden in the `SizeProperty` annotation.

## Base operator seeder

Populates the database with the base OPERATOR user.

Implements `CommandLineRunner` and executes once on application start.
Is instantiated when the `app.user.base-operator.seeder.enabled` property is set to `true` (enabled by default).

Receives a base operator username and password on initialization.
Throws `IllegalStateException` if the credentials are null or blank.

On execution, checks if base operator already exists.
If user exists but is not an OPERATOR, throws `IllegalStateException`.
If exists and is OPERATOR, does nothing.
Otherwise, creates base OPERATOR user (create user operation).
