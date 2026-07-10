# Eazi Cut Business Requirements Document (BRD)

**Document Version:** 1.0

**Prepared By:** Mubeejoy Technologies

**Project:** Eazi Cut Digital Platform

**Status:** Approved

---

# 1. Introduction

## Purpose

This Business Requirements Document (BRD) defines the business needs, objectives, functional requirements, operational workflows, and success criteria for the Eazi Cut Digital Platform.

It serves as the single source of truth between stakeholders, designers, developers, testers, and AI development assistants throughout the project lifecycle.

---

# 2. Business Overview

Eazi Cut is a premium African menswear and bespoke tailoring brand specializing in:

- Bespoke tailoring
- Luxury ready-to-wear clothing
- Wedding attire
- Corporate fashion
- Native wear
- Professional menswear

The business combines traditional tailoring craftsmanship with modern digital experiences to provide customers with a seamless purchasing and customization journey.

---

# 3. Business Goals

The platform should help Eazi Cut achieve the following goals:

## Primary Goals

- Increase brand awareness
- Increase online revenue
- Expand customer reach nationally and internationally
- Improve customer experience
- Streamline tailoring operations
- Build customer loyalty
- Position Eazi Cut as a premium luxury fashion brand

---

## Secondary Goals

- Reduce manual order management
- Simplify measurement collection
- Improve communication with customers
- Increase repeat purchases
- Enable online consultations
- Showcase craftsmanship through a premium digital experience

---

# 4. Business Objectives

The website should:

✓ Generate online sales

✓ Accept secure online payments

✓ Display luxury collections

✓ Support bespoke tailoring

✓ Store customer measurements

✓ Track customer orders

✓ Provide customer accounts

✓ Allow administrators to manage products and orders

---

# 5. Stakeholders

## Primary Stakeholders

Business Owner

Website Administrator

Sales Staff

Tailors

Customers

---

## Secondary Stakeholders

Delivery Partners

Payment Providers

Marketing Team

Customer Support

Future Franchise Partners

---

# 6. Target Audience

Primary

- Executives
- Professionals
- Grooms
- Wedding Guests
- Corporate Clients
- Fashion Enthusiasts

Secondary

- Students
- Diaspora Nigerians
- Returning Customers

Future

- International Customers
- Corporate Organizations
- Fashion Retail Partners

---

# 7. Business Rules

## Orders

Every order must belong to a registered customer.

Every order must contain at least one product or tailoring request.

Every order receives a unique reference number.

---

## Payments

Orders are not considered confirmed until payment is successful.

Failed payments should not create duplicate orders.

Every payment must have a transaction reference.

---

## Measurements

Customers may save multiple measurement profiles.

Measurements may be reused for future orders.

Customers can update measurements at any time.

---

## Products

Only administrators may create or modify products.

Products can be marked:

Active

Inactive

Out of Stock

Featured

---

## Reviews

Only verified customers may submit reviews.

Reviews require approval before publication.

---

# 8. Functional Requirements

## Customer Features

Customer Registration

Login

Forgot Password

Profile Management

Wishlist

Shopping Cart

Checkout

Order History

Order Tracking

Saved Measurements

Review Products

Newsletter Subscription

---

## Product Features

Browse Collections

Product Search

Filtering

Sorting

Product Gallery

Related Products

Availability Status

Product Reviews

---

## Tailoring Features

Book Bespoke Tailoring

Upload Design Inspiration

Select Fabric

Choose Style

Save Measurements

Request Consultation

Track Tailoring Progress

---

## Payment Features

Paystack Integration

Payment Verification

Order Confirmation

Email Receipt

Payment History

Refund Support (Future)

---

## Admin Features

Dashboard

Manage Products

Manage Categories

Manage Orders

Manage Customers

Manage Payments

Manage Reviews

Manage Content

Analytics Dashboard

Newsletter Management

Inventory Monitoring

---

# 9. Non-Functional Requirements

Performance

- Page load under 3 seconds
- Optimized images
- Lazy loading

Security

- JWT Authentication
- HTTPS
- Password Encryption
- Secure Payment Processing

Accessibility

- WCAG AA Compliance
- Keyboard Navigation
- Proper Color Contrast
- Screen Reader Support

Scalability

- Modular Architecture
- RESTful APIs
- Reusable Components
- Cloud Deployment Ready

---

# 10. Customer Workflow

Customer visits homepage

↓

Explores collections

↓

Views product

↓

Adds product to cart

OR

Starts bespoke tailoring

↓

Provides measurements

↓

Reviews order

↓

Makes payment

↓

Receives confirmation

↓

Production begins

↓

Tracks progress

↓

Receives delivery

↓

Leaves review

---

# 11. Tailoring Workflow

Customer submits request

↓

Tailor reviews request

↓

Measurements confirmed

↓

Fabric selected

↓

Production starts

↓

Quality inspection

↓

Packaging

↓

Delivery

↓

Customer confirmation

---

# 12. Product Catalog Requirements

Every product must contain:

Product Name

SKU

Description

Price

Images

Category

Available Sizes

Available Colors

Fabric Information

Stock Status

Estimated Delivery Time

SEO Metadata

---

# 13. Measurement Requirements

Measurement profiles should support:

Chest

Shoulder

Sleeve Length

Neck

Waist

Hip

Trouser Length

Thigh

Inseam

Height

Weight (Optional)

Notes

Reference Images

Multiple Saved Profiles

---

# 14. Order Requirements

Each order should include:

Customer

Order Number

Products

Measurements

Tailoring Requests

Fabric Choice

Payment Status

Production Status

Delivery Address

Tracking Number

Invoice

Estimated Delivery Date

---

# 15. Dashboard Requirements

The Admin Dashboard should provide:

Revenue Overview

Recent Orders

Inventory Status

Customer Growth

Best Selling Products

Pending Orders

Production Queue

Low Stock Alerts

Website Analytics

Payment Summary

---

# 16. Success Criteria

The project will be considered successful if it:

Provides a premium customer experience

Supports online sales

Supports bespoke tailoring

Reduces manual work

Improves customer satisfaction

Strengthens the Eazi Cut brand

Scales for future growth

---

# 17. Out of Scope (Version 1)

The following are planned for future releases:

Mobile Applications

AI Styling Assistant

Fashion Academy

Affiliate Program

Gift Cards

Subscription Boxes

Virtual Try-On

Franchise Portal

International Warehousing

Multi-language Support

---

# 18. Future Expansion

Future versions may include:

AI Measurement Assistance

Appointment Booking

Fabric Marketplace

Customer Loyalty Program

Corporate Uniform Portal

Fashion Community

Mobile App

International Shipping

Virtual Showroom

AR Clothing Preview

---

# 19. Business Success Metrics (KPIs)

Monthly Revenue

Conversion Rate

Average Order Value

Repeat Customer Rate

Customer Satisfaction Score (CSAT)

Net Promoter Score (NPS)

Website Traffic

Checkout Completion Rate

Order Fulfillment Time

Customer Retention Rate

---

# 20. Approval

This document represents the agreed business requirements for the Eazi Cut Digital Platform.

All future design, engineering, testing, and deployment activities should align with the requirements defined in this document.
