# Overview

## 1. Index
1. [Overview](1-overview.md) (this)
2. [Entities](2-entities.md)
3. [Security](3-security.md)
4. [Api Contract](4-api_contract.md)
5. [Operations](5-operations.md)
6. [Error Handling](6-error_handling.md)
7. [Testing](7-testing.md)

## 2. Goals

Create a REST API for a simple inventory management system with JWT authentication and asynchronous execution for slow operations.

**Requirements:**
Users:
- User registration, password change and deletion.
- User separation into customers and operators.
- User login, access refresh, logout and logout all.
- User authentication with JWT and access renewal with refresh tokens.
- Users have a login limit (max sessions).
- Users can only manage their own accounts.

Products:
- Product CRUD operations and restocking for operators.
- Customers can view all products.

Orders:
- Order CRUD operations and lifecycle management for customers.
- Customers can manage only their orders.
- Operators can view all and moderate orders.
- Operators cannot create or manage their own orders.
- Customers have a max active order limit.

Order lifecycle:
- Orders follow the lifecycle DRAFT -> SUBMITTED -> COMPLETED -> CLOSED, and CANCELLED from SUBMITTED or COMPLETED.
- Orders can be modified or deleted only during the DRAFT state.
- SUBMITTED orders get filled when there's available product stock in order of time creation (FIFO).
- Fully filled orders become COMPLETED.
- COMPLETED orders can be validated by the customer to become CLOSED.
- Customers can cancel their SUBMITTED and COMPLETED orders (CANCELLED).
- Operators can cancel customer orders (moderation).
- If an ordered product is deleted the order becomes CANCELLED.
- All product stock from CANCELLED orders is returned.
- Asynchronous execution for stock return and order filling operations.

General:
- Operators receive responses with auditing metadata.
- Pagination and filtering for viewing multiple products, orders or order items.
- Exception handling and custom responses.

## 3. Entities

`User` for credentials and role persistence.

`UserSession` for active session management.

`RefreshToken` for session access renewal and refresh token use tracking.

`Product` for persisting product data.
Has auditing fields.

`Order` for persisting order information and status.
Requires auditing (for filling ordering) and status (DRAFT, SUBMITTED, COMPLETED, CLOSED, CANCELLED) fields.

`OrderItem` for persisting individual products for an order.

**Relationships:**

`User` - one to many - `UserSession`

`UserSession` - one to many - `RefreshToken`

`User` - one to many - `Order`

`Order` - one to many - `OrderItem`

`Product` - one to many - `OrderItem`


## 4. Security

Initial login with user credentials which creates a session and returns a JWT token and a refresh token.
Further authentication uses the provided JWT.
New JWT generated with the provided refresh token on the access refresh endpoint. 

Users access to endpoints (authorization) will be based on their role.
Operators can manage products and view and moderate all orders, but not create or manage their own orders.
Customers can view products and manage their own orders, but cannot manage products or view other people's orders.

Roles are fixed and cannot be changed.
It doesn't make sense to allow registered customers become operators.
New operators will be created by existing operators.

## 5. Available operations

Subject to change.

**User:**
- Create user
- Create operator
- Change password
- Delete user

**Auth:**
- Login
- Refresh access
- Logout
- Logout all

**Product:**
- Create product
- Update product
- Modify product stock
- Get product
- Get products
- Delete product

**Order:**
- Create order
- Add items
- Change item
- Remove item
- Clear items
- Get order
- Get orders
- Delete order

**Order lifecycle:**
- Submit order
- Validate order
- Cancel order

## 6. Exceptions

Global `RestControllerAdvice` to handle application level exceptions and custom `AuthenticationEntryPoint` and `AccessDeniedHandler` to handle security level exceptions.

Reusable error response to keep the error format consistent and predictable.