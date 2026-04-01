# API Documentation

## Swagger

Swagger UI is enabled through Springdoc OpenAPI.

Run the backend:

```bash
cd backend
mvn spring-boot:run
```

Open:

- `http://localhost:8080/swagger-ui.html`

OpenAPI JSON:

- `http://localhost:8080/v3/api-docs`

## Authentication

Protected endpoints use Bearer JWT.

1. Call `POST /api/auth/login`
2. Copy the returned token
3. In Swagger UI, click `Authorize`
4. Enter:

```text
Bearer <your-jwt-token>
```

## Main API Groups

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`

### Users

- `GET /api/users/me`
- `GET /api/users`
- `PATCH /api/users/{userId}/status`

### Tasks

- `POST /api/tasks`
- `PUT /api/tasks/{taskId}`
- `DELETE /api/tasks/{taskId}`
- `POST /api/tasks/{taskId}/submit`
- `POST /api/tasks/{taskId}/assign`
- `POST /api/tasks/{taskId}/approve`
- `POST /api/tasks/{taskId}/reject`
- `POST /api/tasks/{taskId}/comments`
- `GET /api/tasks`
- `GET /api/tasks/{taskId}`

### Audit Logs

- `GET /api/audit-logs`

## Filtering And Pagination

Task listing supports:

- `status`
- `search`
- `page`
- `size`

Example:

```http
GET /api/tasks?status=COMPLETED&search=report&page=0&size=10
```

## Roles

### USER

- manage own tasks
- update own tasks
- submit completed tasks
- comment on own tasks

### ADMIN

- view all tasks
- update any task
- assign tasks
- approve or reject tasks
- manage users
- view audit logs

## Optional Postman

If you want a Postman collection later, it can be generated from the OpenAPI document at:

- `/v3/api-docs`
