# Testing

Overall testing architecture and design choices.

Uses Testcontainers for database layer testing.

Disables the servlet base path for testing for consistent test behavior.

## 1. Unit tests

Unit tests for services and components with isolated functionality.

Uses Mockito for dependency mocking and method call stubbing.

Focus on the main execution paths of each component in isolation.

## 2. Slice tests

Slice tests for controllers, error handling and repositories.

**Controller slice tests**

Uses `@WebMvcTest` with MockMvc to simulate the web layer without actually loading it.

Focus on verifying the successful response, invalid request response and correct security behavior.

**Error handling slice tests**

Same configuration as controller tests (with `@WebMvcTest`). Uses a dummy REST controller to generate the necessary exceptions with method stubbing and request validation.

Focus on correct error response generation.

**Repository slice tests**

Uses `DataJpaTest` with Testcontainers to test on a real database.

Focus on the correct execution of custom and derived repository methods.

## 3. Integration tests

Integration tests for execution flows that either cannot be tested in isolation or require multiple components working together.

## 4. End-to-end tests

End-to-end tests for the full application execution flow.

Loads the full context on a random port and uses Testcontainers for testing with a real database. 

Focus on endpoint happy path and main error responses.