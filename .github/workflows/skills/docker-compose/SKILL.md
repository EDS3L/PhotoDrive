---
name: docker-compose
description: "Create Docker Compose files for projects deployed on a VPS with Traefik reverse proxy. Use when: creating docker-compose.yml, setting up Traefik labels, configuring SSL/TLS, deploying containers behind Traefik, adding new services to VPS, Docker deployment, reverse proxy setup, HTTPS configuration, Let's Encrypt."
argument-hint: 'Describe the project/services you want to deploy (e.g. "Next.js app with API on domain example.com")'
---

# Docker Compose Generator for Traefik VPS

## When to Use

- Creating a new `docker-compose.yml` for any project
- Deploying a new app/service behind Traefik on a VPS
- Adding HTTPS/SSL to a containerized project
- Setting up frontend + API multi-service deployments
- Configuring www → non-www redirects
- Wiring a project into an existing Traefik proxy network

## Architecture Overview

The infrastructure uses a **two-file approach**:

1. **VPS-level compose** — runs Traefik reverse proxy (deployed once on the server)
2. **Project-level compose** — each project gets its own compose file that connects to Traefik via a shared Docker network

All projects share the external network `traefik_proxy_network`.

---

## Procedure

### Step 1: Gather Requirements

Ask the user (if not provided):

- Project name / container name
- Domain(s) for the project
- Services needed (frontend, API, database, etc.)
- Ports each service listens on internally
- Docker image names or build context
- Environment variables needed
- Whether the project needs a www → non-www redirect
- Whether Facebook/social media scraper access is needed

### Step 2: Generate Project Compose File

Use the template and conventions below to create the `docker-compose.yml`.

### Step 3: Remind about `.env`

List all required environment variables the user must define in `.env`.

### Step 4: Verify Network

Remind the user that `traefik_proxy_network` must already exist on the VPS (created by the VPS Traefik compose).

---

## VPS Traefik Compose (Reference)

This is deployed **once** on the VPS. The user should NOT recreate this — it's here for reference only.

```yaml
services:
  traefik:
    image: traefik:v2.11
    container_name: traefik
    restart: unless-stopped
    labels:
      - 'traefik.enable=true'
      - 'traefik.http.middlewares.ratelimit.ratelimit.average=60'
      - 'traefik.http.middlewares.ratelimit.ratelimit.burst=20'
      - 'traefik.http.middlewares.ratelimit.ratelimit.period=60m'
    command:
      - --providers.docker=true
      - --providers.docker.exposedbydefault=false
      - --entrypoints.web.address=:80
      - --entrypoints.websecure.address=:443
      - --entrypoints.web.http.redirections.entrypoint.to=websecure
      - --entrypoints.web.http.redirections.entrypoint.scheme=https
      - --entrypoints.web.http.redirections.entrypoint.permanent=true
      - --entrypoints.web.forwardedHeaders.trustedIPs=127.0.0.1/32,172.16.0.0/12
      - --entrypoints.websecure.forwardedHeaders.trustedIPs=127.0.0.1/32,172.16.0.0/12
      - --certificatesresolvers.my-resolver.acme.httpchallenge=true
      - --certificatesresolvers.my-resolver.acme.httpchallenge.entrypoint=web
      - --certificatesresolvers.my-resolver.acme.email=<USER_EMAIL>
      - --certificatesresolvers.my-resolver.acme.storage=/letsencrypt/acme.json
      - --log.level=INFO
      - --accesslog=true
      - --accesslog.format=json
      - --accesslog.filepath=/var/log/traefik/access.log
    ports:
      - '80:80'
      - '443:443'
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./letsencrypt:/letsencrypt
      - ./logs:/var/log/traefik
    networks:
      - web

networks:
  web:
    name: traefik_proxy_network
```

Key facts about the VPS Traefik setup:

- HTTP→HTTPS redirect is handled globally by Traefik (entrypoint redirect)
- SSL certificates via Let's Encrypt with HTTP challenge
- Certificate resolver name: `my-resolver`
- Entrypoints: `web` (80), `websecure` (443)
- Docker provider with `exposedbydefault=false` — each service must opt-in via `traefik.enable=true`
- Rate limiting middleware available globally: `ratelimit`

---

## Project Compose Template (Backend + Frontend)

This is the **primary template** for most projects (backend + frontend). For frontend-only projects, see the simplified template below.

```yaml
services:
  backend:
    image: ${DOCKER_USERNAME}/<project>-backend:latest
    container_name: <project>-backend
    restart: unless-stopped
    environment:
      DB_URL: ${DB_URL}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      SPRING_PROFILES_ACTIVE: prod
      JAVA_TOOL_OPTIONS: '-Xmx512m -Xms256m'
    deploy:
      resources:
        limits:
          memory: 768M
    networks:
      - internal
    healthcheck:
      test:
        [
          'CMD-SHELL',
          'wget -qO- --spider http://localhost:<BACKEND_PORT>/health || exit 1',
        ]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 40s

  frontend:
    image: ${DOCKER_USERNAME}/<project>-frontend:latest
    container_name: <project>-frontend
    restart: unless-stopped
    depends_on:
      backend:
        condition: service_healthy
    networks:
      - internal
      - web
    labels:
      - 'traefik.enable=true'
      - 'traefik.docker.network=traefik_proxy_network'

      # Main router — HTTPS on primary domain
      - 'traefik.http.routers.<project>.rule=Host(`<domain>`)'
      - 'traefik.http.routers.<project>.entrypoints=websecure'
      - 'traefik.http.routers.<project>.tls.certresolver=my-resolver'
      - 'traefik.http.routers.<project>.service=<project>'
      - 'traefik.http.services.<project>.loadbalancer.server.port=<FRONTEND_PORT>'

      # WWW redirect — handles both HTTP and HTTPS
      - 'traefik.http.routers.<project>-www.rule=Host(`www.<domain>`)'
      - 'traefik.http.routers.<project>-www.entrypoints=web'
      - 'traefik.http.routers.<project>-www.middlewares=<project>-www-redirect'
      - 'traefik.http.routers.<project>-www-secure.rule=Host(`www.<domain>`)'
      - 'traefik.http.routers.<project>-www-secure.entrypoints=websecure'
      - 'traefik.http.routers.<project>-www-secure.middlewares=<project>-www-redirect'
      - 'traefik.http.routers.<project>-www-secure.tls.certresolver=my-resolver'
      - "traefik.http.middlewares.<project>-www-redirect.redirectregex.regex=^https?://www\\.(.*)"
      - 'traefik.http.middlewares.<project>-www-redirect.redirectregex.replacement=https://$${1}'
      - 'traefik.http.middlewares.<project>-www-redirect.redirectregex.permanent=true'

networks:
  internal:
    driver: bridge
  web:
    external: true
    name: traefik_proxy_network
```

## Project Compose Template (Frontend Only)

Simplified template for projects without a backend service.

```yaml
services:
  <service-name>:
    image: ${DOCKER_USERNAME}/<image-name>
    container_name: <service-name>
    restart: unless-stopped
    environment:
      - EXAMPLE_VAR=${EXAMPLE_VAR}
    networks:
      - web
    labels:
      - 'traefik.enable=true'

      - 'traefik.http.routers.${PROJECT}.rule=Host(`${DOMAIN}`)'
      - 'traefik.http.routers.${PROJECT}.entrypoints=websecure'
      - 'traefik.http.routers.${PROJECT}.tls.certresolver=my-resolver'
      - 'traefik.http.routers.${PROJECT}.service=${PROJECT}'
      - 'traefik.http.services.${PROJECT}.loadbalancer.server.port=<INTERNAL_PORT>'

      - 'traefik.http.routers.${PROJECT}-www.rule=Host(`www.${DOMAIN}`)'
      - 'traefik.http.routers.${PROJECT}-www.entrypoints=websecure'
      - 'traefik.http.routers.${PROJECT}-www.tls.certresolver=my-resolver'
      - 'traefik.http.routers.${PROJECT}-www.middlewares=${PROJECT}-www-redirect'
      - "traefik.http.middlewares.${PROJECT}-www-redirect.redirectregex.regex=^https://www\\.(.*)"
      - 'traefik.http.middlewares.${PROJECT}-www-redirect.redirectregex.replacement=https://$${1}'
      - 'traefik.http.middlewares.${PROJECT}-www-redirect.redirectregex.permanent=true'

networks:
  web:
    external: true
    name: traefik_proxy_network
```

---

## Conventions & Rules

### Network

- **Always** use `traefik_proxy_network` as the shared external network
- In project compose: declare it as `external: true` with `name: traefik_proxy_network`
- **Backend + Frontend projects**: Use **two networks**:
  - `internal` (bridge) — for backend↔frontend communication
  - `web` (external) — for Traefik to reach the frontend
  - Backend goes on `internal` only (NOT exposed to Traefik)
  - Frontend goes on **both** `internal` and `web`
- **Frontend-only projects**: Use only the `web` network

### Labels

- Always set `traefik.enable=true` on the service that Traefik routes to (frontend)
- **`traefik.docker.network=traefik_proxy_network`** — REQUIRED when a container is on multiple networks (tells Traefik which network to use for health checks/routing)
- Router names use the project name for uniqueness across projects
- Always use `websecure` entrypoint (HTTP→HTTPS is handled by Traefik globally)
- Always use `my-resolver` as the TLS cert resolver
- Define the loadbalancer server port matching the container's internal port

### `$PROJECT` Variable

- The `$PROJECT` env variable is **optional** — it can be used in labels for dynamic naming, or the project name can be hardcoded directly in router/service names
- Frontend-only projects typically use `${PROJECT}` for flexibility
- Backend+frontend projects often hardcode the project name directly (e.g., `traefik.http.routers.ordovita.rule=...`)
- Choose whichever approach fits the project

### Backend / Internal Services

- Backend services do **NOT** get Traefik labels — they are only reachable via the `internal` network
- They only need to be on the `internal` network
- If a backend needs to be publicly accessible (e.g., public API), add Traefik labels with a subdomain (e.g., `api.domain.com`) and put it on `web` too

### Environment Variables

- Use `${VARIABLE}` syntax with `.env` file
- Use `${VAR:-default}` for optional variables with defaults
- Both formats are valid: `KEY: ${VALUE}` (map) and `- KEY=${VALUE}` (list)
- Common variables: `DOCKER_USERNAME`, `PROJECT` (optional), `DOMAIN` / `PORTFOLIO_DOMAIN`, mail config, DB config, API keys

### Image Naming

- Backend+frontend pattern: `${DOCKER_USERNAME}/<project>-backend` and `${DOCKER_USERNAME}/<project>-frontend`
- Frontend-only pattern: `${DOCKER_USERNAME}/eds3l-${PROJECT}`
- API-only pattern: `${DOCKER_USERNAME}/eds3l-${PROJECT}-api`
- Adapt the prefix/pattern to the user's naming convention

### depends_on & Healthcheck

- Use `depends_on` when frontend depends on backend being available
- **Prefer `condition: service_healthy`** over simple `depends_on` — ensures the backend is actually ready
- Add a `healthcheck` on the backend service:
  ```yaml
  healthcheck:
    test:
      [
        'CMD-SHELL',
        'wget -qO- --spider http://localhost:<PORT>/health || exit 1',
      ]
    interval: 15s
    timeout: 5s
    retries: 5
    start_period: 40s
  ```
- Adjust the health endpoint (`/health`, `/swagger-ui/index.html`, `/actuator/health`, etc.) to match the backend

### Resource Limits

- For Java/heavy backends, set memory limits to prevent OOM:
  ```yaml
  deploy:
    resources:
      limits:
        memory: 768M
  ```
- Also set JVM memory flags via `JAVA_TOOL_OPTIONS: "-Xmx512m -Xms256m"` for Java backends

### Facebook / Social Scraper Access

- If the project needs Open Graph / social media previews, add a priority router:

```yaml
- 'traefik.http.routers.${PROJECT}-fb.rule=Host(`${DOMAIN}`) && HeadersRegexp(`User-Agent`,`.*(facebookexternalhit|Facebot).*`)'
- 'traefik.http.routers.${PROJECT}-fb.entrypoints=websecure'
- 'traefik.http.routers.${PROJECT}-fb.tls.certresolver=my-resolver'
- 'traefik.http.routers.${PROJECT}-fb.service=${PROJECT}'
- 'traefik.http.routers.${PROJECT}-fb.priority=1000'
```

---

## Example 1: Backend + Frontend (Primary Pattern)

This is the **most common** project structure. Backend is isolated on `internal`, frontend bridges both networks.

```yaml
services:
  backend:
    image: ${DOCKER_USERNAME}/ordovita-backend:latest
    container_name: ordovita-backend
    restart: unless-stopped
    environment:
      DB_URL: ${DB_URL}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      MAIL_HOST: ${MAIL_HOST}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      SPRING_PROFILES_ACTIVE: prod
      JAVA_TOOL_OPTIONS: '-Xmx512m -Xms256m'
    deploy:
      resources:
        limits:
          memory: 768M
    networks:
      - internal
    healthcheck:
      test:
        [
          'CMD-SHELL',
          'wget -qO- --spider http://localhost:8080/swagger-ui/index.html || exit 1',
        ]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 40s

  frontend:
    image: ${DOCKER_USERNAME}/ordovita-frontend:latest
    container_name: ordovita-frontend
    restart: unless-stopped
    depends_on:
      backend:
        condition: service_healthy
    networks:
      - internal
      - web
    labels:
      - 'traefik.enable=true'
      - 'traefik.docker.network=traefik_proxy_network'

      - 'traefik.http.routers.ordovita.rule=Host(`ordovita.pl`)'
      - 'traefik.http.routers.ordovita.entrypoints=websecure'
      - 'traefik.http.routers.ordovita.tls.certresolver=my-resolver'
      - 'traefik.http.routers.ordovita.service=ordovita'
      - 'traefik.http.services.ordovita.loadbalancer.server.port=80'

      # www redirect — both HTTP and HTTPS
      - 'traefik.http.routers.ordovita-www.rule=Host(`www.ordovita.pl`)'
      - 'traefik.http.routers.ordovita-www.entrypoints=web'
      - 'traefik.http.routers.ordovita-www.middlewares=ordovita-www-redirect'
      - 'traefik.http.routers.ordovita-www-secure.rule=Host(`www.ordovita.pl`)'
      - 'traefik.http.routers.ordovita-www-secure.entrypoints=websecure'
      - 'traefik.http.routers.ordovita-www-secure.middlewares=ordovita-www-redirect'
      - 'traefik.http.routers.ordovita-www-secure.tls.certresolver=my-resolver'
      - "traefik.http.middlewares.ordovita-www-redirect.redirectregex.regex=^https?://www\\.(.*)"
      - 'traefik.http.middlewares.ordovita-www-redirect.redirectregex.replacement=https://$${1}'
      - 'traefik.http.middlewares.ordovita-www-redirect.redirectregex.permanent=true'

networks:
  internal:
    driver: bridge
  web:
    external: true
    name: traefik_proxy_network
```

Required `.env`:

```env
DOCKER_USERNAME=your-dockerhub-username
DB_URL=jdbc:postgresql://...
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
MAIL_HOST=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
GEMINI_API_KEY=...
```

## Example 2: Frontend Only (with `$PROJECT` variable)

Simplified pattern when there's no backend, using `$PROJECT` for dynamic naming.

```yaml
services:
  myapp:
    image: ${DOCKER_USERNAME}/eds3l-${PROJECT}
    container_name: myapp
    restart: unless-stopped
    environment:
      - MAIL_HOST=${MAIL_HOST:-smtp.mailgun.org}
      - MAIL_PORT=${MAIL_PORT:-587}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
      - MAIL_RECIPIENT=${MAIL_RECIPIENT}
    networks:
      - web
    labels:
      - 'traefik.enable=true'
      - 'traefik.http.routers.${PROJECT}.rule=Host(`${PORTFOLIO_DOMAIN}`)'
      - 'traefik.http.routers.${PROJECT}.entrypoints=websecure'
      - 'traefik.http.routers.${PROJECT}.tls.certresolver=my-resolver'
      - 'traefik.http.routers.${PROJECT}.service=${PROJECT}'
      - 'traefik.http.services.${PROJECT}.loadbalancer.server.port=3000'

      - 'traefik.http.routers.${PROJECT}-www.rule=Host(`www.${PORTFOLIO_DOMAIN}`)'
      - 'traefik.http.routers.${PROJECT}-www.entrypoints=websecure'
      - 'traefik.http.routers.${PROJECT}-www.tls.certresolver=my-resolver'
      - 'traefik.http.routers.${PROJECT}-www.middlewares=${PROJECT}-www-redirect'
      - "traefik.http.middlewares.${PROJECT}-www-redirect.redirectregex.regex=^https://www\\.(.*)"
      - 'traefik.http.middlewares.${PROJECT}-www-redirect.redirectregex.replacement=https://$${1}'
      - 'traefik.http.middlewares.${PROJECT}-www-redirect.redirectregex.permanent=true'

networks:
  web:
    external: true
    name: traefik_proxy_network
```

Required `.env` for this example:

```env
DOCKER_USERNAME=your-dockerhub-username
PROJECT=myapp
PORTFOLIO_DOMAIN=myapp.com
MAIL_HOST=smtp.mailgun.org
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_RECIPIENT=...
```

## WWW Redirect Patterns

### Full redirect (HTTP + HTTPS) — recommended for backend+frontend projects

Handles `www.` on both `web` (HTTP) and `websecure` (HTTPS) entrypoints:

```yaml
# HTTP www redirect
- 'traefik.http.routers.<project>-www.rule=Host(`www.<domain>`)'
- 'traefik.http.routers.<project>-www.entrypoints=web'
- 'traefik.http.routers.<project>-www.middlewares=<project>-www-redirect'
# HTTPS www redirect
- 'traefik.http.routers.<project>-www-secure.rule=Host(`www.<domain>`)'
- 'traefik.http.routers.<project>-www-secure.entrypoints=websecure'
- 'traefik.http.routers.<project>-www-secure.middlewares=<project>-www-redirect'
- 'traefik.http.routers.<project>-www-secure.tls.certresolver=my-resolver'
# Shared middleware
- "traefik.http.middlewares.<project>-www-redirect.redirectregex.regex=^https?://www\\.(.*)"
- 'traefik.http.middlewares.<project>-www-redirect.redirectregex.replacement=https://$${1}'
- 'traefik.http.middlewares.<project>-www-redirect.redirectregex.permanent=true'
```

Note the regex uses `https?://` to match both protocols.

### Simple redirect (HTTPS only) — sufficient for frontend-only projects

Since Traefik already handles HTTP→HTTPS globally, only the `websecure` www router is needed:

```yaml
- 'traefik.http.routers.${PROJECT}-www.rule=Host(`www.${DOMAIN}`)'
- 'traefik.http.routers.${PROJECT}-www.entrypoints=websecure'
- 'traefik.http.routers.${PROJECT}-www.tls.certresolver=my-resolver'
- 'traefik.http.routers.${PROJECT}-www.middlewares=${PROJECT}-www-redirect'
- "traefik.http.middlewares.${PROJECT}-www-redirect.redirectregex.regex=^https://www\\.(.*)"
- 'traefik.http.middlewares.${PROJECT}-www-redirect.redirectregex.replacement=https://$${1}'
- 'traefik.http.middlewares.${PROJECT}-www-redirect.redirectregex.permanent=true'
```
