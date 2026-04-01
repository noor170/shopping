# Features

## Core Application

- JWT-based authentication
- BCrypt password hashing
- Spring Security protected APIs
- Role-based access control with `USER` and `ADMIN`
- User registration and login
- User profile endpoint
- Admin user management
- User active/inactive status management

## Task Management

- Create task
- Update task
- Soft delete task
- View own tasks as `USER`
- View all tasks as `ADMIN`
- Task assignment and reassignment by admin
- Task comments by user and admin
- Task workflow with statuses:
  - `PENDING`
  - `IN_PROGRESS`
  - `COMPLETED`
  - `APPROVED`
  - `REJECTED`
- User can save task progress at any time
- Completed task can be submitted for admin review
- Admin can approve or reject submitted tasks

## Audit And Tracking

- `createdBy`
- `updatedBy`
- `createdAt`
- `updatedAt`
- Separate audit logs for key actions

## API And Documentation

- Swagger / OpenAPI documentation
- API documentation Markdown
- Database schema documentation
- ERD in Markdown
- SQL DDL in Markdown
- Setup and architecture documentation

## Frontend

- React dashboard
- User panel
- Admin panel
- Task edit modal
- Filtering
- Pagination
- Comment UI

## Testing

- Unit tests
- Integration tests
- MockMvc API coverage for major endpoints

## DevOps

- Docker for backend
- Docker for frontend
- Docker Compose
- GitHub Actions CI/CD
- Backend test pipeline
- Frontend build pipeline
- Docker image build and push workflow

## Seed Data

- Initial admin and multiple user accounts
- Seeded tasks in multiple workflow states
- Seeded comments
- Seeded audit logs
