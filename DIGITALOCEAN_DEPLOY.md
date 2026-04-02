# DigitalOcean Deploy

## App Spec

This repository now includes an App Platform spec:

- [digitalocean-app.yaml](/Users/macbookairm1/Documents/GitHub/demo/shopping/digitalocean-app.yaml)

It is configured for this GitHub repository:

- `noor170/shopping`

## What It Deploys

- `backend` as a Java service from `backend/`
- `frontend` as a static site from `frontend/`

## Important Source Directories

DigitalOcean must use:

- backend source directory: `/backend`
- frontend source directory: `/frontend`

That avoids the root-level build file detection problem.

## How To Deploy

1. Push the latest code to GitHub.
2. In DigitalOcean App Platform, choose to create an app from an existing app spec.
3. Upload or reference `digitalocean-app.yaml`.
4. Confirm GitHub access is still enabled for the `noor170/shopping` repository.
5. Deploy.

## Notes

- The frontend now uses `VITE_API_URL` and defaults to `/api`.
- Local development still works because Vite proxies `/api` to `http://localhost:8080`.
- Swagger remains available through the backend routes.
