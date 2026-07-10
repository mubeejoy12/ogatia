# User Flows

Document ID: PRD-009

Category: Product Design

Version: 1.0

Status: Approved

Owner: Mubeejoy Technologies

Project: Eazi Cut Digital Platform

---

# Purpose

This document defines every important user flow within the Eazi Cut platform.

Every page, API endpoint, validation rule, and interaction should support these flows.

The objective is to minimize friction and create a premium customer experience.

---

# Flow 1 — Browse Products

```mermaid
flowchart LR
A[Homepage] --> B[Shop]
B --> C[Apply Filters]
C --> D[Product Details]
D --> E[Add to Cart]
```

Goal

Allow customers to quickly discover products.

---

# Flow 2 — Buy Ready-to-Wear Product

```mermaid
flowchart LR
A[Homepage]
--> B[Shop]
--> C[Product]
--> D[Select Size]
--> E[Add to Cart]
--> F[Checkout]
--> G[Payment]
--> H[Order Confirmation]
```

Requirements

- Size selection required
- Quantity selection
- Stock validation
- Secure payment
- Confirmation email

---

# Flow 3 — Bespoke Tailoring

```mermaid
flowchart LR
A[Homepage]
--> B[Bespoke]
--> C[Measurement Guide]
--> D[Fill Measurements]
--> E[Choose Fabric]
--> F[Review]
--> G[Checkout]
--> H[Payment]
--> I[Production]
```

Requirements

- Measurements saved
- Images optional
- Fabric selection
- Style selection

---

# Flow 4 — User Registration

```mermaid
flowchart LR
A[Register]
--> B[Fill Form]
--> C[Validate]
--> D[Email Verification]
--> E[Login]
```

Requirements

- Email validation
- Password strength
- Duplicate account check

---

# Flow 5 — Login

```mermaid
flowchart LR
A[Login]
--> B[Authentication]
--> C{Role}
C --> D[Customer Dashboard]
C --> E[Tailor Dashboard]
C --> F[Admin Dashboard]
```

---

# Flow 6 — Forgot Password

```mermaid
flowchart LR
A[Forgot Password]
--> B[Enter Email]
--> C[Email Link]
--> D[Reset Password]
--> E[Login]
```

---

# Flow 7 — Customer Dashboard

```mermaid
flowchart LR
Dashboard
--> Orders
Dashboard --> Measurements
Dashboard --> Wishlist
Dashboard --> Profile
Dashboard --> Settings
```

---

# Flow 8 — Tailor Dashboard

```mermaid
flowchart LR
Dashboard
--> Orders
Orders --> Accept
Accept --> Production
Production --> Ready
Ready --> Delivered
```

Order Status

NEW

↓

ACCEPTED

↓

IN_PROGRESS

↓

READY

↓

DELIVERED

---

# Flow 9 — Admin Product Management

```mermaid
flowchart LR
Admin
--> Products
Products --> Create
Products --> Edit
Products --> Delete
Products --> Publish
```

---

# Flow 10 — Checkout

```mermaid
flowchart LR
Cart
--> Shipping
--> Review
--> Payment
--> Success
```

Validation

- Stock available
- Address valid
- Payment successful

---

# Flow 11 — Order Tracking

```mermaid
flowchart LR
Order Created
--> Paid
--> In Production
--> Quality Check
--> Shipped
--> Delivered
```

Timeline displayed to customer.

---

# Flow 12 — Product Search

```mermaid
flowchart LR
Search
--> Suggestions
--> Results
--> Product
```

Supports

- Product names
- Categories
- Fabrics
- Collections

---

# Flow 13 — Wishlist

```mermaid
flowchart LR
Product
--> Save
--> Wishlist
--> Move to Cart
```

---

# Flow 14 — Reviews

```mermaid
flowchart LR
Delivered
--> Leave Review
--> Rating
--> Publish
```

Only verified customers may review products.

---

# Flow 15 — Contact Support

```mermaid
flowchart LR
Customer
--> Contact Form
--> Support Team
--> Resolution
```

---

# Error Flows

## Payment Failure

```mermaid
flowchart LR
Payment
--> Failed
--> Retry
--> Success
```

---

## Out of Stock

```mermaid
flowchart LR
Add to Cart
--> Stock Check
--> Out of Stock
--> Notify Customer
```

---

## Session Expired

```mermaid
flowchart LR
Protected Page
--> Session Expired
--> Login
--> Return to Previous Page
```

---

# UX Principles

Every flow should:

- Require the fewest possible steps.
- Provide clear feedback.
- Allow users to recover from errors.
- Never lose user data unexpectedly.
- Maintain a premium, polished experience.

---

# Success Metrics

- Registration completed in under 2 minutes.
- Checkout completed in under 3 minutes.
- Product discoverable within 3 clicks.
- Order status always visible.
- Customers can recover from errors without contacting support.

---

# Final Principle

Every user flow should feel effortless.

Customers should focus on buying great clothing—not figuring out how to use the website.