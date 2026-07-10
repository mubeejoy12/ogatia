# Information Architecture

Document ID: PRD-008

Category: Product Design

Version: 1.0

Status: Approved

Owner: Mubeejoy Technologies

Project: Eazi Cut Digital Platform

---

# Purpose

This document defines the complete structure of the Eazi Cut website.

It specifies:

- Website hierarchy
- Navigation
- URL structure
- User dashboards
- Admin dashboards
- Customer journeys
- Relationships between pages

This serves as the blueprint for frontend development.

---

# Website Structure

Home

├── Shop

├── Collections

├── Bespoke Tailoring

├── Lookbook

├── About

├── Blog

├── Contact

├── Login

├── Register

├── Dashboard

└── Admin

---

# Public Pages

## Home

Purpose

Introduce the brand.

Build trust.

Drive exploration.

Sections

Hero

Featured Collections

Best Sellers

Why Choose Eazi Cut

Testimonials

Craftsmanship

Instagram Feed

Newsletter

Footer

URL

/

---

## Shop

URL

/shop

Purpose

Browse all products.

Filters

Category

Price

Color

Size

Availability

Sorting

Newest

Popular

Price

---

## Product Details

URL

/shop/[slug]

Contains

Images

Description

Fabric

Reviews

Related Products

Size Guide

Add to Cart

Wishlist

---

## Collections

URL

/collections

Examples

Wedding

Native Wear

Corporate

Luxury

New Arrivals

Seasonal

---

## Collection Details

/collections/[slug]

---

## Bespoke Tailoring

URL

/bespoke

Contains

How it Works

Measurement Guide

Book Consultation

Tailoring Process

CTA

---

## Lookbook

URL

/lookbook

Purpose

Fashion inspiration.

Gallery.

Campaigns.

---

## About

/about

Story

Mission

Vision

Founder

Workshop

Craftsmanship

---

## Contact

/contact

Contact Form

Phone

WhatsApp

Google Maps

Business Hours

Social Media

---

## Blog

/blog

Fashion Tips

Style Guides

Fabric Education

Wedding Advice

News

---

## Blog Post

/blog/[slug]

---

# Customer Authentication

Login

/login

Register

/register

Forgot Password

/forgot-password

Reset Password

/reset-password

Email Verification

/verify-email

---

# Customer Dashboard

/dashboard

Contains

Overview

Orders

Measurements

Wishlist

Addresses

Profile

Settings

Notifications

---

Dashboard Structure

Dashboard

├── Overview

├── My Orders

├── Order Details

├── Measurements

├── Wishlist

├── Saved Addresses

├── Payment History

├── Profile

├── Settings

└── Logout

---

# Orders

/dashboard/orders

View

Current Orders

Past Orders

Cancelled Orders

Order Tracking

---

Order Details

/dashboard/orders/[id]

---

Measurements

/dashboard/measurements

Create

Edit

Delete

Reuse

---

Wishlist

/dashboard/wishlist

---

Addresses

/dashboard/addresses

---

Profile

/dashboard/profile

---

Settings

/dashboard/settings

---

# Checkout Flow

Cart

/cart

↓

Checkout

/checkout

↓

Payment

/payment

↓

Confirmation

/order-success

↓

Tracking

/dashboard/orders/[id]

---

# Tailor Dashboard

/tailor

Sections

Overview

Orders

Measurements

Customers

Portfolio

Reviews

Availability

Settings

Revenue

Analytics

---

Tailor Dashboard Structure

Overview

Orders

Production Queue

Measurements

Portfolio

Messages

Calendar

Revenue

Reviews

Settings

---

# Admin Dashboard

/admin

Sections

Dashboard

Products

Categories

Collections

Orders

Customers

Tailors

Payments

Reviews

Users

Analytics

Settings

CMS

Newsletter

Media Library

Audit Logs

---

Admin Navigation

Dashboard

Products

Orders

Customers

Payments

Tailors

Analytics

CMS

Media

Settings

---

# Footer Navigation

Shop

Collections

Bespoke

Lookbook

About

Contact

FAQ

Privacy Policy

Terms

Returns

Shipping

Blog

Newsletter

Social Media

---

# Header Navigation

Logo

Home

Shop

Collections

Bespoke

Lookbook

About

Blog

Search

Wishlist

Cart

Profile

---

# Search

/search

Supports

Products

Collections

Blog

Lookbook

Suggestions

Recent Searches

---

# Notifications

/dashboard/notifications

---

# URL Principles

Use lowercase URLs.

Use hyphens.

Avoid IDs where possible.

Prefer slugs.

Example

Good

/shop/luxury-senator-wear

Bad

/shop/product?id=23

---

# Navigation Principles

Users should always know:

Where they are.

How they got there.

Where they can go next.

---

# Breadcrumb Example

Home

>

Collections

>

Native Wear

>

Royal Agbada

---

# Future Expansion

Appointment Booking

Virtual Fitting

AI Stylist

Gift Cards

Membership

Affiliate Portal

Corporate Portal

Academy

---

# Success Criteria

Every page should have a clear purpose.

Every route should be predictable.

Every navigation path should reduce friction.

Users should reach any destination within three clicks whenever practical.

---

# Final Principle

Information Architecture is not about pages.

It is about helping users find what they need effortlessly.