# Task Management System

Full-stack assignment solution using Java 21, Spring Boot, Spring Security, JWT, React, and H2.

## Demo Video

View the project demo video here:

- [Watch Demo Video on Google Drive](https://drive.google.com/file/d/1KItoWmh5zJ51xQYw-sN7dVVY2BvsC9Yf/view?usp=sharing)

Detailed setup and architecture notes:

- [FEATURES.md](/Users/macbookairm1/Documents/GitHub/demo/shopping/FEATURES.md)
- [SETUP_AND_ARCHITECTURE.md](/Users/macbookairm1/Documents/GitHub/demo/shopping/SETUP_AND_ARCHITECTURE.md)
- [API_DOCUMENTATION.md](/Users/macbookairm1/Documents/GitHub/demo/shopping/API_DOCUMENTATION.md)
- [DATABASE_SCHEMA.md](/Users/macbookairm1/Documents/GitHub/demo/shopping/DATABASE_SCHEMA.md)
- [DOCKER_SETUP.md](/Users/macbookairm1/Documents/GitHub/demo/shopping/DOCKER_SETUP.md)
- [DIGITALOCEAN_DEPLOY.md](/Users/macbookairm1/Documents/GitHub/demo/shopping/DIGITALOCEAN_DEPLOY.md)

CI/CD:

- GitHub Actions workflow: [.github/workflows/ci-cd.yml](/Users/macbookairm1/Documents/GitHub/demo/shopping/.github/workflows/ci-cd.yml)

## Stack

- Backend: Spring Boot 3.4, Spring Security, JPA, H2, JWT, BCrypt
- Frontend: React 18 + Vite

## Features

- JWT authentication and Spring Security authorization
- RBAC with `USER` and `ADMIN`
- Task workflow with assignment, comments, approval, and rejection
- Audit fields and audit logs
- Pagination and filtering
- Swagger / OpenAPI documentation
- Database schema and ERD documentation
- Docker and Docker Compose support
- GitHub Actions CI/CD
- Unit and integration tests
- Seed data for testing on initial startup

## Project Structure

- `backend` - REST API with authentication, RBAC, task workflow, soft delete, pagination, filtering, and audit logs
- `frontend` - React UI for login, registration, task management, admin review, and audit visibility

## Backend Run

```bash
cd backend
mvn spring-boot:run
```

Backend jar packaging:

```bash
cd backend
mvn clean package
```

Seeded accounts:

- `admin / Admin@123`
- `user / User@123`

H2 console:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:taskdb`
- Username: `sa`
- Password: empty

## Frontend Run

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

- `http://localhost:5173`

## Core APIs

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/users` admin only
- `PATCH /api/users/{id}/status` admin only
- `POST /api/tasks`
- `POST /api/tasks` admin may include `assigneeUserId` to assign the task to an active user during creation
- `PUT /api/tasks/{id}` user can save/update own task at any time with `PENDING`, `IN_PROGRESS`, or `COMPLETED`; admin can update any non-deleted task
- `DELETE /api/tasks/{id}` soft delete, including admin delete across all tasks
- `POST /api/tasks/{id}/assign` admin only, reassign task to an active user
- `POST /api/tasks/{id}/comments` user or admin can comment on accessible tasks
- `POST /api/tasks/{id}/submit` submits an already `COMPLETED` task for admin review
- `POST /api/tasks/{id}/approve` admin only
- `POST /api/tasks/{id}/reject` admin only
- `GET /api/tasks?status=COMPLETED&search=api&page=0&size=10`
- `GET /api/audit-logs` admin only

## Security Notes

- JWT is validated in `JwtAuthenticationFilter`, which extracts the bearer token, verifies the signature and expiry through `JwtService`, and places the authenticated user into Spring Security's context.
- Authorization is enforced in two layers:
  - `SecurityConfig` secures all routes except auth, health, and H2 console.
  - `@PreAuthorize` on service/controller methods enforces role-based access such as admin-only user management and task review.

## Audit Tracking

- Every entity extending `BaseEntity` stores `createdBy`, `updatedBy`, `createdAt`, and `updatedAt`.
- Task and user API responses include those fields so the audit trail is visible to clients.
- Key actions are written to `audit_logs`, including registration, login, task create/update/assign/complete/approve/reject/delete, and user status changes.
- Task comments are stored with a user reference plus `createdAt`/`updatedAt`, and are returned inside task responses.
