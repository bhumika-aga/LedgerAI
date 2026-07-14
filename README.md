# LedgerAI

An AI-powered productivity platform for Chartered Accountants, CPAs, auditors, and accounting professionals.

LedgerAI helps to account professionals understand financial documents, summarize reports, answer accounting-related
questions, generate professional outputs, and automate repetitive work using AI.

> **LedgerAI is not an ERP or accounting software.**
>
> It works alongside existing accounting systems such as Tally, QuickBooks, Xero, Zoho Books, SAP, Oracle Financials,
> and Microsoft Excel.

---

# Vision

Accounting professionals spend countless hours reading documents, extracting information, drafting emails, comparing
financial statements, and researching regulations.

LedgerAI aims to reduce those manual tasks from hours to minutes through AI-assisted workflows.

Our goal is to build the everyday AI workspace for accounting professionals.

---

# MVP Features

Version 1 includes:

* Secure Authentication
* User Profiles
* Client Management
* Document Upload
* Document Storage
* OCR for scanned documents
* AI Document Summarization
* AI Chat with uploaded documents
* AI-generated Email Drafts
* Report Generation
* Global Search
* Activity Timeline

---

# Technology Stack

## Frontend

* React
* TypeScript
* Vite
* Material UI
* React Router
* React Query
* Axios

## Backend

* Java 21
* Spring Boot 3
* Spring Security
* Spring Data JPA
* Hibernate
* Maven

## Database

* PostgreSQL (Neon)

## Storage

* Cloudinary or Supabase Storage (TBD)

## Documentation

* OpenAPI (Swagger)

## Authentication

* JWT Access Tokens
* Refresh Tokens

## Hosting

Frontend:

* Vercel

Backend:

* Render

Database:

* Neon PostgreSQL

---

# Repository Structure

```text
ledger-ai/

├── CLAUDE.md                 # Engineering constitution for Claude Code
├── README.md
│
├── backend/                  # Spring Boot application
│
├── frontend/                 # React + TypeScript application
│
├── docs/
│   ├── PRD.md
│   ├── SRS.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── API_SPEC.md
│   ├── SECURITY.md
│   ├── UI_UX.md
│   ├── ROADMAP.md
│   └── decisions/
│
├── docker/
│
└── .github/
```

---

# Engineering Principles

LedgerAI follows these principles:

* Clean Architecture (applied pragmatically)
* SOLID Principles
* Layered Architecture
* Domain-oriented package organization
* RESTful APIs
* Secure-by-default development
* Testable code
* Maintainable code
* Clear documentation

Every major engineering decision should be documented through an Architecture Decision Record (ADR).

---

# Development Workflow

Every feature should follow this sequence:

1. Requirements
2. Design
3. Database Changes
4. API Design
5. Backend Implementation
6. Unit Testing
7. Frontend Implementation
8. Documentation Update
9. Code Review

Features should be delivered in small, incremental milestones.

---

# Coding Standards

General Guidelines

* Use meaningful naming.
* Prefer readability to cleverness.
* Keep controllers thin.
* Keep business logic inside services.
* Keep repositories focused on persistence.
* Validate all external input.
* Centralize exception handling.
* Avoid duplicate logic.
* Write self-documenting code.

---

# Documentation

Project documentation lives under the `docs` directory.

Core documents include:

* Product Requirements Document (PRD)
* Software Requirements Specification (SRS)
* System Architecture
* Database Design
* API Specification
* Security Design
* UI/UX Specification
* Development Roadmap

All architectural decisions should be captured as ADRs.

---

# Branching Strategy

Recommended Git workflow:

* `main` – Production-ready code
* `develop` – Integration branch
* `feature/<feature-name>` – New features
* `bugfix/<issue-name>` – Bug fixes
* `hotfix/<issue-name>` – Production fixes

Pull requests should target `develop` unless fixing a production issue.

---

# Project Milestones

## Milestone 0

* Repository setup
* Documentation
* Architecture
* CI/CD
* Docker configuration

## Milestone 1

* Authentication
* User Management

## Milestone 2

* Client Management

## Milestone 3

* Document Upload
* File Storage

## Milestone 4

* AI Document Analysis

## Milestone 5

* AI Chat

## Milestone 6

* Reports
* Search
* Activity Timeline

## Milestone 7

* Production deployment
* Beta release

---

# Long-Term Vision

Future versions may include:

* Tally integration
* QuickBooks integration
* Xero integration
* Compliance reminders
* Multi-document reasoning
* Bank statement analysis
* Financial statement comparison
* AI audit workpapers
* Multi-country accounting support
* AI workflow automation

These are intentionally out of scope for Version 1.

---

# Contributing

This project prioritizes:

* Simplicity
* Maintainability
* Scalability
* Security
* High-quality engineering practices

Every contribution should improve the long-term health of the codebase rather than introducing unnecessary complexity.

---

# License

License to be decided before public release.
