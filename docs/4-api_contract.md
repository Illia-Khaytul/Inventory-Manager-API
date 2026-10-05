# API Contract

Design and definition of the API contract and its request and response data.

Base endpoint path is `/api/v1`.

**Common response DTOs:**

`paginated response`<T>:
- int `page`
- int `pageSize`
- int `totalPages`
- long `totalElements`
- List<T> `elements`

## 1. Authentication API

**Endpoints:**
1. **Login** : user credentials authentication, opens a new session, returns a JWT access token and a refresh token
2. **Refresh access** : uses up the refresh token, returns a new JWT and refresh token
3. **Logout** : idempotent logout, closes the session by refresh token
4. **Logout all** : idempotent logout, closes all open sessions for user

**Request DTOs:**

`login request`:
- String `username`: required, not blank, max length 50
- String `password`: required, not blank, max length 50

`refresh token request`:
- String `refreshToken`: required, not blank, max length 50

**Response DTOs:**

`access token response`:
- String `issuer`
- Instant `issuedAt`
- Instant `expiresAt`
- String `subject`
- String `role`
- String `accessToken`
- String `refreshToken`

### 1.1. Login

**POST** `/auth/login`

Does not require authentication.

**Receives:**
- Body: `login request`

**Returns:**
- Body: `access token response`

**Responses:**
- 200 OK: user authenticated and new session opened
- 400 Bad Request: request validation failed
- 401 Unauthorized: failed to authenticate with provided credentials
- 403 Forbidden: exceeds max open session limit

### 1.2. Refresh access

**POST** `/auth/refresh-access`

Does not require authentication.

**Receives:**
- Body: `refresh token request`

**Returns:**
- Body: `access token response`

**Error Responses:**
- 200 OK: refresh token valid and new access token generated
- 400 Bad Request: request validation failed
- 401 Unauthorized: used, invalid or expired refresh token (can't use this token to gain access)
- 409 Conflict: refresh token has been modified concurrently

### 1.3. Logout

**POST** `/auth/logout`

Requires authentication.

**Receives:**
- Body: `refresh token request`

**Returns:** nothing

**Error Responses:**
- 204 No Content: performed logout operation
- 400 Bad Request: request validation failed
- 401 Unauthorized: not authenticated

### 1.4. Logout all

**POST** `/auth/logout-all`

Requires authentication.

**Receives:** nothing

**Returns:** nothing

**Error Responses:**
- 204 No Content: performed logout all operation
- 401 Unauthorized: not authenticated

## 2. User management API

**Endpoints:**
1. **Create customer** : creates a new user with the CUSTOMER role
2. **Create operator** : creates a new user with the OPERATOR role
3. **Change password** : changes the password of the accessing user
4. **Delete user** : idempotent deletion, deletes the accessing user

**Request DTOs:**

`create user request`:
- String `username`: required, not blank, max length 50
- String `password`: required, not blank, max length 50

`password change request`:
- String `oldPassword`: required, not blank, max length 50
- String `newPassword`: required, not blank, max length 50

**Response DTOs:**

`user response`:
- Long `id`
- String `username`
- UserRole `role`

### 2.1. Create customer

**POST** `/users`

Does not require authentication.

**Receives:**
- Body: `create user request`

**Returns:**
- Body: `user response`

**Responses:**
- 201 Created: new user with CUSTOMER role successfully created
- 400 Bad Request: request validation failed, password is invalid
- 409 Conflict: provided username is not unique

### 2.2. Create operator

**POST** `/users/operators`

Requires authentication as OPERATOR.

**Receives:**
- Body: `create user request`

**Returns:**
- Body: `user response`

**Responses:**
- 201 Created: new user with OPERATOR role successfully created
- 400 Bad Request: request validation failed, password is invalid
- 401 Unauthorized: not authenticated
- 403 Forbidden: authenticated but not an OPERATOR
- 409 Conflict: provided username is not unique

### 2.3. Change password

**PATCH** `/users/password/change`

Requires authentication.

**Receives:**
- Body: `password change request`

**Returns:** nothing

**Responses:**
- 204 No Content: user password changed successfully
- 400 Bad Request: request validation failed, password is invalid
- 401 Unauthorized: not authenticated
- 404 Not Found: authenticated user does not exist
- 409 Conflict: authenticated user got deleted concurrently

### 2.4. Delete user

**DELETE** `/users`

Requires authentication.

**Receives:** nothing

**Returns:** nothing

**Responses:**
- 204 No Content: performed delete operation
- 401 Unauthorized: not authenticated

## 3. Product management API

**Endpoints:**
1. **Create product** : creates a new product, does not set the stock
2. **Update product** : partial update, updates existing product, does not modify stock
3. **Change product stock** : modify existing product stock
4. **Get product** : get an existing product
5. **Get products** : get a page of existing products
6. **Delete product** : idempotent deletion, deletes an existing product

**Request DTOs:**

`create product request`:
- String `name`: required, not blank, max length 100
- String `description`: optional, max length 1000
- BigDecimal `price`: required, min 0, max integer limit, mapped from a String

`update product request`:
- String `name`: optional, max length 100
- String `description`: optional, max length 1000
- BigDecimal `price`: optional, min 0, max integer limit, mapped from a String

`modify stock request`:
- int `stockChange`: required, integer value range

`product filtering`:
- String `nameContains`: optional, max length 50
- Integer `minStock`: optional, min 0, max integer limit
- BigDecimal `minPrice`: optional, min 0, max integer limit, mapped from a String
- BigDecimal `maxPrice`: optional, min 0, max integer limit, mapped from a String

**Response DTOs:**

`product response`:
- Long `id`
- String `name`
- String `description`
- int `stock`
- BigDecimal `price`: returned as String
- Instant `createdAt`: only for operators
- String `createdBy`: only for operators
- Instant `modifiedAt`: only for operators
- String `modifiedBy`: only for operators

`product short response`:
- Long `id`
- String `name`
- int `stock`
- BigDecimal `price`: returned as String

### 3.1. Create product

**POST** `/products`

Requires authentication as OPERATOR.

**Receives:**
- Body: `create product request`

**Returns:**
- Header: Location: `/api/v1/products/{productId}`
- Body: `product response`

**Responses:**
- 201 Created: new product created successfully
- 400 Bad Request: request validation failed
- 401 Unauthorized: not authenticated
- 403 Forbidden: authenticated but not an OPERATOR
- 409 Conflict: provided product name is not unique

### 3.2. Update product

**PATCH** `/products/{productId}`

Requires authentication as OPERATOR.

**Receives:**
- Path variable: long `productId`
- Body: `update product request`

**Returns:**
- Body: `product response`

**Responses:**
- 200 OK: product updated successfully
- 400 Bad Request: request validation failed
- 401 Unauthorized: not authenticated
- 403 Forbidden: authenticated but not an OPERATOR
- 404 Not Found: product does not exist by id
- 409 Conflict: new product name is not unique or product was modified concurrently

### 3.3. Change product stock

**PATCH** `/products/{productId}/stock`

Requires authentication as OPERATOR.

**Receives:**
- Path variable: long `productId`
- Body: `modify stock request`

**Returns:**
- Body: `product response`

**Responses:**
- 200 OK: product stock modified successfully
- 400 Bad Request: request validation failed
- 401 Unauthorized: not authenticated
- 403 Forbidden: authenticated but not an OPERATOR
- 404 Not Found: product does not exist by id

### 3.4. Get product

**GET** `/products/{productId}`

Does not require authentication.

**Receives:**
- Path variable: long `productId`

**Returns:**
- Body: `product response`

**Responses:**
- 200 OK: product found
- 400 Bad Request: request validation failed
- 404 Not Found: product does not exist by id

### 3.5. Get products

**GET** `/products`

Does not require authentication.

**Receives:**
- Query parameters: Pageable `pagination`: default page 0, page size 10, sort by `createdAt` DESC
- Query parameters: `product filtering`

**Returns:**
- Body: `paginated response` of `book short response`

**Responses:**
- 200 OK: products page returned
- 400 Bad Request: request validation failed

### 3.6. Delete product

**DELETE** `/products/{productId}`

Requires authentication as OPERATOR.

**Receives:**
- Path variable: long `productId`

**Returns:** nothing

**Responses:**
- 204 No Content: performed delete operation
- 400 Bad Request: request validation failed
- 401 Unauthorized: not authenticated
- 403 Forbidden: authenticated but not an OPERATOR
