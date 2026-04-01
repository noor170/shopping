# Docker Setup

## Overview

This project includes:

- Dockerfile for the Spring Boot backend
- Dockerfile for the React frontend
- `docker-compose.yml` for running both services together

## Files

- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx/default.conf`
- `docker-compose.yml`

## Run With Docker Compose

From the project root:

```bash
docker compose up --build
```

## Services

Backend:

- `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- H2 console: `http://localhost:8080/h2-console`

Frontend:

- `http://localhost:5173`

## Stop Containers

```bash
docker compose down
```

## Rebuild

```bash
docker compose up --build
```

## Notes

- The backend uses the in-memory H2 database, so data is reset when the container restarts.
- The frontend is built with Node 20 and served by Nginx.
- The frontend calls the backend through `http://localhost:8080/api`.
