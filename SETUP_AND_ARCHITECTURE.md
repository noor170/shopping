# Setup And Architecture

## Overview

This project is a full-stack Task Management System built with:

- Java 21
- Spring Boot
- Spring Security
- JWT
- H2 Database
- React

It supports:

- user registration and login
- JWT-based authentication
- role-based authorization
- task creation and updates
- admin assignment, approval, and rejection
- audit tracking
- task comments
- pagination and filtering

## Project Structure

- `backend/` Spring Boot REST API
- `frontend/` React client

## Setup Instructions

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js

Recommended:

- Node.js 20 or newer

## Backend Setup

1. Open a terminal in `backend/`
2. Run:

```bash
mvn spring-boot:run
```

Backend default URL:

- `http://localhost:8080`

H2 console:

- `http://localhost:8080/h2-console`

H2 connection settings:

- JDBC URL: `jdbc:h2:mem:taskdb`
- Username: `sa`
- Password: empty

Seeded accounts:

- Admin: `admin / Admin@123`
- User: `user / User@123`

## Frontend Setup

1. Open a terminal in `frontend/`
2. Install dependencies
3. Start the dev server

Using npm:

```bash
npm install
npm run dev
```

Using Yarn:

```bash
corepack yarn install
corepack yarn dev
```

Frontend default URL:

- `http://localhost:5173`

## Build Commands

Backend tests:

```bash
cd backend
mvn test
```

Backend jar packaging:

```bash
cd backend
mvn clean package
```

Frontend production build:

```bash
cd frontend
corepack yarn build
```

## Architecture Explanation

## High-Level Design

The application is split into two layers:

1. React frontend
2. Spring Boot backend

The frontend handles login, dashboard views, task editing, filtering, pagination, comments, and admin operations.

The backend exposes secure REST APIs and contains the business logic, persistence layer, and authorization rules.

## Backend Architecture

The backend follows a layered structure:

- `controller` handles HTTP requests and responses
- `service` contains business rules
- `repository` handles database access through Spring Data JPA
- `entity` defines database models
- `dto` defines request and response payloads
- `security` contains JWT and Spring Security configuration
- `exception` centralizes API error handling
- `config` contains auditing and seed configuration

### Security Flow

1. User logs in through `/api/auth/login`
2. Backend validates credentials using Spring Security
3. JWT token is generated and returned
4. Client sends `Authorization: Bearer <token>`
5. `JwtAuthenticationFilter` validates the token
6. Spring Security stores the authenticated user in the security context
7. Protected APIs use the authenticated identity and role

### Authorization Model

- `USER`
  - manage own tasks
  - update own task status
  - submit completed tasks
  - comment on own tasks

- `ADMIN`
  - view all tasks
  - update any task
  - assign and reassign tasks
  - approve or reject completed submitted tasks
  - manage user status
  - view audit logs

Authorization is enforced in:

- `SecurityConfig`
- service methods with `@PreAuthorize`

## Task Workflow

User-editable statuses:

- `PENDING`
- `IN_PROGRESS`
- `COMPLETED`

Review statuses:

- `APPROVED`
- `REJECTED`

Workflow:

1. User creates a task
2. User updates title, description, and status as needed
3. User can move the task between `PENDING`, `IN_PROGRESS`, and `COMPLETED`
4. Once ready, user submits the completed task
5. Admin reviews the submitted completed task
6. Admin approves or rejects it

## Audit Design

Auditing exists in two forms:

### Entity audit fields

Shared entity metadata comes from `BaseEntity`:

- `createdBy`
- `updatedBy`
- `createdAt`
- `updatedAt`

### Audit log records

Separate audit log entries capture key system events such as:

- registration
- login
- task creation
- task update
- task assignment
- task submission
- task approval
- task rejection
- task deletion
- task comments
- user status change

## Database Design

Core entities:

- `User`
- `Task`
- `TaskComment`
- `AuditLog`

Relationships:

- one user can own many tasks
- one task can have many comments
- one user can author many comments
- one admin can review many tasks

## Frontend Architecture

The frontend is a React single-page interface with:

- authentication screen
- task dashboard
- task edit modal
- comments section
- admin management panels

The client communicates with the backend using `fetch` through `src/api.js`.

State is managed locally with React hooks:

- auth state
- task list state
- pagination/filter state
- edit modal state
- comment draft state

## API Summary

Authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`

Users:

- `GET /api/users/me`
- `GET /api/users`
- `PATCH /api/users/{id}/status`

Tasks:

- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`
- `POST /api/tasks/{id}/submit`
- `POST /api/tasks/{id}/assign`
- `POST /api/tasks/{id}/approve`
- `POST /api/tasks/{id}/reject`
- `POST /api/tasks/{id}/comments`
- `GET /api/tasks`
- `GET /api/tasks/{id}`

Audit:

- `GET /api/audit-logs`

## Notes

- H2 is in-memory, so data resets when the backend restarts.
- CORS is configured for the local frontend dev server.
- The frontend works best with a modern Node.js runtime.
