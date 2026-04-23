---
name: github-actions-deploy
description: 'Create GitHub Actions CI/CD workflows for building Docker images and deploying to VPS. Use when: creating GitHub workflow, CI/CD pipeline, Docker build and push, deploy to VPS via SSH, GitHub Actions, continuous deployment, automated deployment, build pipeline, Docker Hub push, selective builds, monorepo CI.'
argument-hint: 'Describe the project to deploy (e.g. "Next.js app with API, monorepo subfolder portfolio-hub")'
---

# GitHub Actions CI/CD Workflow Generator

## When to Use

- Creating a new `.github/workflows/*.yml` for any project
- Setting up CI/CD pipeline that builds Docker images and deploys to VPS
- Configuring selective builds based on changed files (monorepo)
- Automating Docker Hub push + VPS deployment via SSH
- Adding a new service (frontend, API) to an existing workflow

## Architecture Overview

The workflow follows a **4-stage pipeline**:

1. **Detect changes** — determine which parts of the project changed (frontend, backend, compose)
2. **Build & push frontend** — build Docker image, push to Docker Hub (only if changed)
3. **Build & push backend** — build backend Docker image, push to Docker Hub (only if changed)
4. **Deploy to VPS** — copy compose file via SCP, SSH in, pull images, run `docker compose up`

Each stage runs conditionally — only when relevant files changed.

Most projects have both a **backend** and **frontend**. The primary template reflects this.

---

## Procedure

### Step 1: Gather Requirements

Ask the user (if not provided):

- Project name
- Project subfolder path in the repo (or root)
- Services to build (frontend, backend, workers, etc.)
- Frontend framework (Next.js, React, etc.) and Node.js version needed
- Backend framework (Spring Boot, Express, etc.)
- Whether type-checking or linting should run before build (frontend)
- Docker Hub as registry (or other)
- Domain / VPS target folder name
- List of secrets needed (DB, JWT, mail, API keys, OAuth, etc.)

### Step 2: Generate Workflow File

Create `.github/workflows/<project-name>.yml` using the template and conventions below.

### Step 3: List Required GitHub Secrets

Provide the user a list of all secrets to configure in GitHub repo settings.

### Step 4: Verify Prerequisites

Remind the user:

- Dockerfile(s) must exist for each service
- `docker-compose-prod.yml` must exist in the project folder
- SSH key must be configured for VPS access
- Docker Hub account with access token

---

## Workflow Template (Backend + Frontend — Primary)

This is the **most common** pattern. Adapt paths, image names, and secrets to your project.

```yaml
name: <Project Name> — Build, Publish & Deploy

on:
  workflow_dispatch:
  push:
    branches: [main]
    paths:
      - '<frontend-folder>/**'
      - '<backend-folder>/**'
      - 'docker-compose-prod.yml'

jobs:
  # ── 1. Detect what changed ──
  changes:
    runs-on: ubuntu-latest
    outputs:
      frontend: ${{ steps.filter.outputs.frontend }}
      backend: ${{ steps.filter.outputs.backend }}
      compose: ${{ steps.filter.outputs.compose }}
    steps:
      - uses: actions/checkout@v4

      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            frontend:
              - '<frontend-folder>/**'
            backend:
              - '<backend-folder>/**'
            compose:
              - 'docker-compose-prod.yml'

  # ── 2. Build & push frontend image ──
  build-frontend:
    needs: changes
    if: needs.changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Optional: Node.js validation (only for Next.js/React/TS frontends)
      # - name: Set up Node.js
      #   uses: actions/setup-node@v5
      #   with:
      #     node-version: '22'
      #     cache: 'npm'
      #     cache-dependency-path: <frontend-folder>/package-lock.json
      # - name: Install & validate
      #   working-directory: ./<frontend-folder>
      #   run: |
      #     npm ci
      #     npx tsc --noEmit

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          registry: docker.io
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_TOKEN }}

      - name: Build and push frontend
        uses: docker/build-push-action@v6
        with:
          context: ./<frontend-folder>
          file: ./<frontend-folder>/Dockerfile
          push: true
          # build-args: |
          #   EXPO_PUBLIC_API_URL=
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/<project-name>-frontend:latest
            ${{ secrets.DOCKER_USERNAME }}/<project-name>-frontend:${{ github.sha }}
          cache-from: type=gha,scope=frontend
          cache-to: type=gha,scope=frontend,mode=max

  # ── 3. Build & push backend image ──
  build-backend:
    needs: changes
    if: needs.changes.outputs.backend == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Optional: Java/Gradle validation (for Spring Boot backends)
      # - name: Set up JDK 21
      #   uses: actions/setup-java@v4
      #   with:
      #     distribution: temurin
      #     java-version: '21'
      # - name: Run tests
      #   working-directory: ./<backend-folder>
      #   run: ./gradlew test --no-daemon

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          registry: docker.io
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_TOKEN }}

      - name: Build and push backend
        uses: docker/build-push-action@v6
        with:
          context: ./<backend-folder>
          file: ./<backend-folder>/Dockerfile
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/<project-name>-backend:latest
            ${{ secrets.DOCKER_USERNAME }}/<project-name>-backend:${{ github.sha }}
          cache-from: type=gha,scope=backend
          cache-to: type=gha,scope=backend,mode=max

  # ── 4. Deploy to VPS ──
  deploy:
    needs: [changes, build-frontend, build-backend]
    if: |
      always() &&
      (needs.build-frontend.result == 'success' || needs.build-frontend.result == 'skipped') &&
      (needs.build-backend.result == 'success' || needs.build-backend.result == 'skipped') &&
      (needs.changes.outputs.frontend == 'true' || needs.changes.outputs.backend == 'true' || needs.changes.outputs.compose == 'true')
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Copy docker-compose to VPS
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          key: ${{ secrets.VPS_SSH_KEY }}
          source: 'docker-compose-prod.yml'
          target: '/home/${{ secrets.VPS_USERNAME }}/<vps-project-dir>'

      - name: Deploy to VPS
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            set -e

            echo "${{ secrets.DOCKER_TOKEN }}" | docker login docker.io -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin

            cd /home/${{ secrets.VPS_USERNAME }}/<vps-project-dir>

            export DOCKER_USERNAME="${{ secrets.DOCKER_USERNAME }}"
            # Export all project-specific secrets here

            docker pull ${{ secrets.DOCKER_USERNAME }}/<project-name>-frontend:latest
            docker pull ${{ secrets.DOCKER_USERNAME }}/<project-name>-backend:latest

            docker compose -f docker-compose-prod.yml up -d --remove-orphans

            docker image prune -f
```

---

## Conventions & Rules

### Triggers

- **Always** include `workflow_dispatch` for manual trigger
- **Always** trigger on `push` to `main` branch
- **Always** use `paths` filter to avoid unnecessary builds
- Include all files that affect the build: source code, config, Dockerfile, compose, lock files

### Change Detection (Job 1)

- Use `dorny/paths-filter@v3` to detect what changed
- Define separate filter groups: `frontend`, `backend`, `compose` (add more as needed)
- Pass outputs to downstream jobs via `needs.changes.outputs.<name>`
- Path filters in the `changes` job should mirror the `on.push.paths` trigger paths
- Folder names are flexible — can be `client/server`, `frontend/backend`, or any other naming

### Build Jobs (Jobs 2, 3)

- Each service gets its **own build job** (frontend and backend build in parallel)
- **Conditional execution**: `if: needs.changes.outputs.<name> == 'true'`
- Use `actions/checkout@v5` in every job
- Docker build pattern:
  - `docker/setup-buildx-action@v3` for Buildx
  - `docker/login-action@v3` for Docker Hub login
  - `docker/build-push-action@v6` for build + push
- **Always** use GHA cache: `cache-from: type=gha` + `cache-to: type=gha,mode=max`
- **Always** tag with both `:latest` and `:${{ github.sha }}`

### Pre-Build Validation (Optional per service)

Validation steps run **before** the Docker build to catch errors early. They are optional — if the Dockerfile handles everything, skip them.

#### Node.js / TypeScript frontend:

```yaml
- name: Set up Node.js
  uses: actions/setup-node@v5
  with:
    node-version: '22'
    cache: 'npm'
    cache-dependency-path: <path>/package-lock.json

- name: Install & type-check
  working-directory: ./<path>
  run: |
    npm ci
    npx tsc --noEmit
```

#### Java / Spring Boot backend:

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: '21'

- name: Run tests
  working-directory: ./<path>
  run: ./gradlew test --no-daemon
```

#### Pure Docker (no validation):

- Skip straight to Buildx → login → build+push
- The Dockerfile handles all compilation internally
- This is valid for **both** frontend and backend

### Docker Build Args

- Use `build-args` when the image needs build-time configuration:
  ```yaml
  build-args: |
    EXPO_PUBLIC_API_URL=
  ```
- Empty values are valid (set at build time, overridden at runtime)

### GHA Cache Scoping

- When a workflow builds **multiple images**, use `scope` to avoid cache collisions:
  ```yaml
  cache-from: type=gha,scope=frontend
  cache-to: type=gha,scope=frontend,mode=max
  ```
- Without scope, use the default: `cache-from: type=gha` / `cache-to: type=gha,mode=max`
- **Rule**: If there are 2+ build jobs in one workflow → always scope the caches

### Image Naming

- Backend+frontend pattern: `${{ secrets.DOCKER_USERNAME }}/<project-name>-backend:latest` and `<project-name>-frontend:latest`
- Frontend-only pattern: `${{ secrets.DOCKER_USERNAME }}/eds3l-<project-name>:latest`
- Adapt the prefix/pattern to the user's naming convention

### Deploy Job (Job 4)

- **Must depend on all build jobs**: `needs: [changes, build-frontend, build-backend]`
- **Must use `always()` condition** so deploy still runs if only compose changed (builds skipped):
  ```yaml
  if: |
    always() &&
    (needs.build-frontend.result == 'success' || needs.build-frontend.result == 'skipped') &&
    (needs.build-backend.result == 'success' || needs.build-backend.result == 'skipped') &&
    (needs.changes.outputs.frontend == 'true' || needs.changes.outputs.backend == 'true' || needs.changes.outputs.compose == 'true')
  ```
- **SCP step**: Copy compose file to VPS using `appleboy/scp-action@v0.1.7`
  - Use `strip_components: 1` when compose is in a **subfolder** (strips the subfolder prefix)
  - **Omit** `strip_components` when compose is at the **repo root**
- **SSH step**: Deploy using `appleboy/ssh-action@v1.0.3`
  - `set -e` at the start to fail on any error
  - Docker login on VPS
  - `cd` to project directory on VPS
  - Export all environment variables from secrets
  - `docker pull` each image explicitly (`:latest` tag)
  - `docker compose -f docker-compose-prod.yml up -d --remove-orphans`
  - `docker image prune -f` to clean up old images
  - Some env vars may be set to **empty string** instead of a secret (e.g., `OAUTH2_FRONTEND_URL=""`)

### VPS Directory Structure

- Each project lives in `/home/<VPS_USERNAME>/<project-dir>/`
- The compose file lands as `docker-compose-prod.yml` in that directory
- No `.env` file on VPS — all variables are exported inline from GitHub secrets

### Folder Naming

- Folder names for frontend and backend are **flexible** — adapt to the project structure:
  - `client/` + `server/` (e.g., Ordovita)
  - `frontend/` + `backend/`
  - `portfolio-hub/` + `portfolio-hub/api/` (monorepo subfolder)
- The path filters, build context, and Dockerfile paths must match the actual project structure

### Secrets

These secrets **must** be configured in GitHub repository settings:

| Secret             | Description                                 | Required                           |
| ------------------ | ------------------------------------------- | ---------------------------------- |
| `DOCKER_USERNAME`  | Docker Hub username                         | Always                             |
| `DOCKER_TOKEN`     | Docker Hub access token                     | Always                             |
| `VPS_HOST`         | VPS IP address or hostname                  | Always                             |
| `VPS_USERNAME`     | SSH username on VPS                         | Always                             |
| `VPS_SSH_KEY`      | Private SSH key for VPS access              | Always                             |
| `PORTFOLIO_DOMAIN` | Domain for the project                      | Optional (if hardcoded in compose) |
| `PROJECT`          | Project identifier (used in compose labels) | Optional (if hardcoded in compose) |

Add project-specific secrets as needed:

- **DB**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- **Auth**: `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `OAUTH2_FRONTEND_URL`
- **Mail**: `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_RECIPIENT`
- **AI/APIs**: `GEMINI_API_KEY`, `GROQ_API_KEY`, `AI_PROVIDER`

---

## Variations

### Project Without Backend (Frontend Only)

Remove the `build-backend` job:

- Remove `backend` from path filters and change detection
- Remove `build-backend` from deploy's `needs`
- Remove the backend `docker pull` from the deploy script
- Simplify the deploy `if` condition
- Use `eds3l-<project>` image naming if preferred
- No need for cache scoping with a single build job

### Project Without Frontend (Backend/API Only)

Remove the `build-frontend` job:

- Remove `frontend` from path filters and change detection
- Remove `build-frontend` from deploy's `needs`
- Remove the frontend `docker pull` from the deploy script

### Multiple Services

Add additional `build-and-push-<service>` jobs following the same pattern:

- Add path filter group in `changes` job
- Create a new build job conditioned on that filter
- Add to deploy's `needs` array and `if` condition
- Add `docker pull` for the new image in deploy script

### Non-TypeScript / Non-Node Frontend

- Remove the `Set up Node.js` and `Install & validate` steps from frontend build
- Go straight from checkout to Docker Buildx setup (just like backend)

### Monorepo: Frontend in Subfolder with Shared Config Files

When frontend is not in a separate `/frontend` folder but has config files at project root:

```yaml
frontend:
  - '<project-folder>/src/**'
  - '<project-folder>/app/**'
  - '<project-folder>/package.json'
  - '<project-folder>/package-lock.json'
  - '<project-folder>/Dockerfile'
  - '<project-folder>/next.config.mjs'
  - '<project-folder>/tailwind.config.js'
  - '<project-folder>/tsconfig.json'
backend:
  - '<project-folder>/api/**'
```

---

## Example 1: Backend + Frontend (Spring Boot + React, root-level compose)

Real-world pattern: `client/` = frontend, `server/` = Java backend with Gradle tests, compose at repo root, scoped caches.

```yaml
name: Ordovita — Build, Publish & Deploy

on:
  workflow_dispatch:
  push:
    branches: [main]
    paths:
      - 'server/src/**'
      - 'server/build.gradle'
      - 'server/settings.gradle'
      - 'server/Dockerfile'
      - 'client/app/**'
      - 'client/components/**'
      - 'client/lib/**'
      - 'client/package.json'
      - 'client/package-lock.json'
      - 'client/Dockerfile'
      - 'client/nginx.conf'
      - 'client/tailwind.config.js'
      - 'client/tsconfig.json'
      - 'docker-compose-prod.yml'

jobs:
  changes:
    runs-on: ubuntu-latest
    outputs:
      frontend: ${{ steps.filter.outputs.frontend }}
      backend: ${{ steps.filter.outputs.backend }}
      compose: ${{ steps.filter.outputs.compose }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            frontend:
              - 'client/app/**'
              - 'client/components/**'
              - 'client/lib/**'
              - 'client/package.json'
              - 'client/package-lock.json'
              - 'client/Dockerfile'
              - 'client/nginx.conf'
              - 'client/tailwind.config.js'
              - 'client/tsconfig.json'
            backend:
              - 'server/src/**'
              - 'server/build.gradle'
              - 'server/settings.gradle'
              - 'server/Dockerfile'
            compose:
              - 'docker-compose-prod.yml'

  build-frontend:
    needs: changes
    if: needs.changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          registry: docker.io
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_TOKEN }}

      - name: Build and push frontend
        uses: docker/build-push-action@v6
        with:
          context: ./client
          file: ./client/Dockerfile
          push: true
          build-args: |
            EXPO_PUBLIC_API_URL=
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/ordovita-frontend:latest
            ${{ secrets.DOCKER_USERNAME }}/ordovita-frontend:${{ github.sha }}
          cache-from: type=gha,scope=frontend
          cache-to: type=gha,scope=frontend,mode=max

  build-backend:
    needs: changes
    if: needs.changes.outputs.backend == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Run tests
        working-directory: ./server
        run: ./gradlew test --no-daemon

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          registry: docker.io
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_TOKEN }}

      - name: Build and push backend
        uses: docker/build-push-action@v6
        with:
          context: ./server
          file: ./server/Dockerfile
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/ordovita-backend:latest
            ${{ secrets.DOCKER_USERNAME }}/ordovita-backend:${{ github.sha }}
          cache-from: type=gha,scope=backend
          cache-to: type=gha,scope=backend,mode=max

  deploy:
    needs: [changes, build-frontend, build-backend]
    if: |
      always() &&
      (needs.build-frontend.result == 'success' || needs.build-frontend.result == 'skipped') &&
      (needs.build-backend.result == 'success' || needs.build-backend.result == 'skipped') &&
      (needs.changes.outputs.frontend == 'true' || needs.changes.outputs.backend == 'true' || needs.changes.outputs.compose == 'true')
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Copy docker-compose to VPS
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          key: ${{ secrets.VPS_SSH_KEY }}
          source: 'docker-compose-prod.yml'
          target: '/home/${{ secrets.VPS_USERNAME }}/ordovita'

      - name: Deploy to VPS
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            set -e

            echo "${{ secrets.DOCKER_TOKEN }}" | docker login docker.io -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin

            cd /home/${{ secrets.VPS_USERNAME }}/ordovita

            export DOCKER_USERNAME="${{ secrets.DOCKER_USERNAME }}"
            export DB_URL="${{ secrets.DB_URL }}"
            export DB_USERNAME="${{ secrets.DB_USERNAME }}"
            export DB_PASSWORD="${{ secrets.DB_PASSWORD }}"
            export JWT_SECRET="${{ secrets.JWT_SECRET }}"
            export MAIL_HOST="${{ secrets.MAIL_HOST }}"
            export MAIL_USERNAME="${{ secrets.MAIL_USERNAME }}"
            export MAIL_PASSWORD="${{ secrets.MAIL_PASSWORD }}"
            export GEMINI_API_KEY="${{ secrets.GEMINI_API_KEY }}"
            export GROQ_API_KEY="${{ secrets.GROQ_API_KEY }}"
            export AI_PROVIDER="${{ secrets.AI_PROVIDER }}"
            export GOOGLE_CLIENT_ID="${{ secrets.GOOGLE_CLIENT_ID }}"
            export GOOGLE_CLIENT_SECRET="${{ secrets.GOOGLE_CLIENT_SECRET }}"
            export OAUTH2_FRONTEND_URL=""

            docker pull ${{ secrets.DOCKER_USERNAME }}/ordovita-frontend:latest
            docker pull ${{ secrets.DOCKER_USERNAME }}/ordovita-backend:latest

            docker compose -f docker-compose-prod.yml up -d --remove-orphans

            docker image prune -f
```

## Example 2: Frontend Only with Node.js Validation + Lightweight API (Monorepo subfolder)

Monorepo pattern: project in `portfolio-hub/` subfolder, Node.js frontend validation, lightweight API, `strip_components` for SCP.

```yaml
name: Nasz Wielki Dzień — Build, Publish & Deploy

on:
  workflow_dispatch:
  push:
    branches: [main]
    paths:
      - 'portfolio-hub/src/**'
      - 'portfolio-hub/app/**'
      - 'portfolio-hub/package.json'
      - 'portfolio-hub/package-lock.json'
      - 'portfolio-hub/Dockerfile'
      - 'portfolio-hub/next.config.mjs'
      - 'portfolio-hub/tailwind.config.js'
      - 'portfolio-hub/tsconfig.json'
      - 'portfolio-hub/docker-compose-prod.yml'
      - 'portfolio-hub/api/**'

jobs:
  changes:
    runs-on: ubuntu-latest
    outputs:
      app: ${{ steps.filter.outputs.app }}
      api: ${{ steps.filter.outputs.api }}
      compose: ${{ steps.filter.outputs.compose }}
    steps:
      - uses: actions/checkout@v5
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            app:
              - 'portfolio-hub/src/**'
              - 'portfolio-hub/app/**'
              - 'portfolio-hub/package.json'
              - 'portfolio-hub/package-lock.json'
              - 'portfolio-hub/Dockerfile'
              - 'portfolio-hub/next.config.mjs'
              - 'portfolio-hub/tailwind.config.js'
              - 'portfolio-hub/tsconfig.json'
            api:
              - 'portfolio-hub/api/**'
            compose:
              - 'portfolio-hub/docker-compose-prod.yml'

  build-and-push:
    needs: changes
    if: needs.changes.outputs.app == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5

      - name: Set up Node.js 22
        uses: actions/setup-node@v5
        with:
          node-version: '22'
          cache: 'npm'
          cache-dependency-path: portfolio-hub/package-lock.json

      - name: Install & type-check (validate)
        working-directory: ./portfolio-hub
        run: |
          npm ci
          npx tsc --noEmit

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          registry: docker.io
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_TOKEN }}

      - name: Build and push image
        uses: docker/build-push-action@v6
        with:
          context: ./portfolio-hub
          file: ./portfolio-hub/Dockerfile
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/eds3l-naszwielkidzien:latest
            ${{ secrets.DOCKER_USERNAME }}/eds3l-naszwielkidzien:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  build-and-push-api:
    needs: changes
    if: needs.changes.outputs.api == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          registry: docker.io
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_TOKEN }}

      - name: Build and push API image
        uses: docker/build-push-action@v6
        with:
          context: ./portfolio-hub/api
          file: ./portfolio-hub/api/Dockerfile
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/eds3l-naszwielkidzien-api:latest
            ${{ secrets.DOCKER_USERNAME }}/eds3l-naszwielkidzien-api:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    needs: [changes, build-and-push, build-and-push-api]
    if: |
      always() &&
      (needs.build-and-push.result == 'success' || needs.build-and-push.result == 'skipped') &&
      (needs.build-and-push-api.result == 'success' || needs.build-and-push-api.result == 'skipped') &&
      (needs.changes.outputs.app == 'true' || needs.changes.outputs.api == 'true' || needs.changes.outputs.compose == 'true')
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5

      - name: Copy docker-compose to VPS
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          key: ${{ secrets.VPS_SSH_KEY }}
          source: 'portfolio-hub/docker-compose-prod.yml'
          target: '/home/${{ secrets.VPS_USERNAME }}/naszwielkidzien'
          strip_components: 1

      - name: Deploy to VPS
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            set -e

            echo "${{ secrets.DOCKER_TOKEN }}" | docker login docker.io -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin

            cd /home/${{ secrets.VPS_USERNAME }}/naszwielkidzien

            export DOCKER_USERNAME="${{ secrets.DOCKER_USERNAME }}"
            export PORTFOLIO_DOMAIN="${{ secrets.PORTFOLIO_DOMAIN }}"
            export MAIL_HOST="${{ secrets.MAIL_HOST }}"
            export MAIL_PORT="${{ secrets.MAIL_PORT }}"
            export MAIL_USERNAME="${{ secrets.MAIL_USERNAME }}"
            export MAIL_PASSWORD="${{ secrets.MAIL_PASSWORD }}"
            export MAIL_RECIPIENT="${{ secrets.MAIL_RECIPIENT }}"
            export PROJECT="${{ secrets.PROJECT }}"

            docker pull ${{ secrets.DOCKER_USERNAME }}/eds3l-naszwielkidzien:latest
            docker pull ${{ secrets.DOCKER_USERNAME }}/eds3l-naszwielkidzien-api:latest

            docker compose -f docker-compose-prod.yml up -d --remove-orphans

            docker image prune -f
```
