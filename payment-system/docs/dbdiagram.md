# DB Diagram IO에서 사용할 ERD 구성 스크립트

```
//// --------------------------------------
//// ERD for E-commerce Order/Payment System
//// --------------------------------------
Table Users as U {
  user_id int [pk, increment]
  email varchar(255) [not null, unique]
  password_hash varchar(255) [not null]
  name varchar(100)
  created_at timestamp [default: `now()`]
}

Table Products as Prod {
  product_id int [pk, increment]
  name varchar(255) [not null]
  price decimal(10, 2) [not null]
  stock int [not null, default: 0]
  description text
  created_at timestamp [default: `now()`]
}

Table Orders as O {
  order_id varchar(255) [pk, note: 'e.g., UUID or custom ID like ord_...']
  user_id int [not null]
  total_amount decimal(10, 2) [not null]
  status varchar(50) [not null, note: 'e.g., PENDING_PAYMENT, COMPLETED, CANCELLED']
  ordered_at timestamp [default: `now()`]
}

Table OrderItems as OI {
  order_item_id int [pk, increment]
  order_id varchar(255) [not null]
  product_id int [not null]
  quantity int [not null]
  price decimal(10, 2) [not null, note: 'Price at the time of order']
}

Table Payments as Pay {
  payment_id int [pk, increment]
  order_id varchar(255) [not null, unique]
  method_id int [note: 'FK to PaymentMethods, nullable for one-time payments']
  imp_uid varchar(255) [unique, note: 'Transaction ID from PortOne']
  amount decimal(10, 2) [not null]
  status varchar(50) [not null, note: 'e.g., PAID, FAILED, REFUNDED, PARTIALLY_REFUNDED']
  payment_method varchar(100)
  paid_at timestamp
}

Table Refunds as R {
  refund_id int [pk, increment]
  payment_id int [not null]
  amount decimal(10, 2) [not null, note: 'Supports partial refunds']
  reason text
  status varchar(50) [not null, note: 'e.g., COMPLETED, FAILED']
  refunded_at timestamp [default: `now()`]
}

// --- Relationships ---
Ref: U.user_id < O.user_id
Ref: Prod.product_id < OI.product_id
Ref: O.order_id < OI.order_id
Ref: O.order_id - Pay.order_id // One-to-One
Ref: Pay.payment_id < R.payment_id
```