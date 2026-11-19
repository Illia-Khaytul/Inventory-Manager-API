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
- UserSession `session`: foreign key for UserSession, LAZY fetching, required, on delete cascade 
- Integer `version`: version

Notes:
- Token value will be hashed.
- Refresh tokens will be persisted along their related user sessions for logging purposes.
- Delete all tokens with their related session.
Refresh tokens are tightly related to user sessions.
There cannot be a refresh token that does not point to a session.
- Versioning to ensure the token gets used (used flag flipped) only once.

## 4. Product

Stores the product data and auditing fields.

**Fields:**
- Long `id`: primary key, identity
- String `name`: required, unique
- String `description`: big string (text)
- int `stock`: required
- BigDecimal `price`: required
- Instant `createdAt`: required, not updatable, auditing field
- String `createdBy`: required, not updatable, auditing field
- Instant `modifiedAt`: auditing field
- String `modifiedBy`: auditing field
- Instant `deletedAt`: deletion auditing field
- String `deletedBy`: deletion auditing field
- Integer `version`: version

Notes:
- Price is BigDecimal because it is a monetary value, it must be precise.
- Uses Spring's entity auditing. 
- The created at/by fields are set once for new entities and cannot be modified.
- Deletion auditing fields are used for product soft deletion.
It simplifies foreign key resolution for order items.
- Versioning for optimistic locking during concurrent modification.

## 5. Order

Stores the order status, total price, it's items and auditing fields. Serves as the aggregate root for order items.

**Fields:**
- Long `id`: primary key, identity
- OrderStatus `status`: required, saved as String
- BigDecimal `totalPrice`: required
- User `customer`: foreign key for User, LAZY fetching, optional, on delete set null
- int `totalItems`: transient, formula
- List<OrderItem> `items`: LAZY fetching, ordered by `totalPrice` descending
- Instant `createdAt`: required, not updatable, auditing field
- String `createdBy`: required, not updatable, auditing field
- Instant `modifiedAt`: auditing field
- String `modifiedBy`: auditing field
- Integer `version`: version

Notes:
- The total price is BigDecimal because it is a monetary value, it must be precise.
- Customer foreign key is nullable.
Some orders must remain persisted on user deletion for record keeping.
- The total items field is transient and required to avoid additional count queries when mapping the order to a response.
Uses Hibernate's Formula annotation to load the total item value with the order directly.
- Uses Spring's entity auditing.
- The created at/by fields are set once for new entities and cannot be modified.
- Versioning for optimistic locking during concurrent modification.
The order is the aggregate root, so all order item operations use its versioning and audit fields.

## 6. OrderItem

Stores the referenced product id and the item data.

**Fields:**
- OrderItemId `id`: composite primary key, consists of the referenced order and product ids
- BigDecimal `totalPrice`: transient
- int `quantity`: required
- int `reserved`: required
- String `productName`: required
- BigDecimal `productPrice`: required
- Order `order`: foreign key for Order, maps id, LAZY fetching, required, on delete cascade
- Product `product`: foreign key for Product, maps id, LAZY fetching, required, on delete no action (default)

Notes:
- Order items depend on both their related order and product to exist, and they cannot be viewed individually.
A composite primary key is used combining the order and product ids.
This additionally ensures that a product can be added to an order only once.
- The total price is transient because it can be calculated from the product price and quantity fields.
It is calculated on load (uses PostLoad).
- Product price and name fields are used as snapshots of the product's values when the order is no longer modifiable.
- When an order gets deleted all of its items get deleted with it.
Items depend on their order and cannot exist independently.
- Products are soft-deleted, their relationship to related order items does not change.