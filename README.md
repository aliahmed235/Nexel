# nexel

A self-hosted **Vercel-style deployment platform**. Connect a GitHub repository and nexel builds it, hosts it, and serves it at a live URL — automatically redeploying on every push, with rollbacks, deployment history, live build logs, and real-time build status.

> Built to understand what actually happens between *"push your code"* and *"it's live."*

**Live:** https://nexel0.vercel.app &nbsp;·&nbsp; **Stack:** Java 17 · Spring Boot 4 · PostgreSQL · Redis · Docker · Cloudflare R2 · GCP · Railway

---

## Table of contents

- [What it does](#what-it-does)
- [Architecture](#architecture)
- [How a deployment works](#how-a-deployment-works)
- [Real-time build status](#real-time-build-status)
- [Why a separate build machine](#why-a-separate-build-machine)
- [Features](#features)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Data model](#data-model)
- [API reference](#api-reference)
- [Security](#security)
- [Running locally](#running-locally)
- [Deployment](#deployment)
- [Roadmap](#roadmap)
- [Credits](#credits)

---

## What it does

```
you                         nexel
───                         ─────
connect a GitHub repo   →   verifies it, assigns a subdomain
push code / click Deploy →  clones it, builds it in an isolated container
                            uploads the built site to object storage
visit the link          →   nexel serves your live site
push again              →   it redeploys automatically (GitHub webhook)
a deploy broke it?      →   roll back to any previous version instantly
```

nexel is a **backend-focused** project: the platform (API + build system + storage + serving) is the work here. The dashboard frontend is a separate React app.

---

## Architecture

nexel is a **small distributed system**, not a single app. The public API and the build worker run on **different machines** and coordinate through Redis. This split is the core design decision — building means running untrusted user code, which is slow and risky, so it never touches the API server.

```
                                   ┌──────────────────────────────┐
                                   │           Frontend           │
                                   │     React + Vite  (Vercel)   │
                                   └───────────────┬──────────────┘
                                                   │  HTTPS · REST + SSE
                                                   ▼
 ┌──────────────────────────────────────────────────────────────────────────────┐
 │                        API  ·  Spring Boot  ·  Railway                         │
 │                                                                                │
 │   Controllers ──▶ Services ──▶ Repositories                                    │
 │   • GitHub OAuth login        • Projects        • Deployments                  │
 │   • Serves built sites        • SSE status stream                              │
 └────────┬────────────────┬─────────────────┬──────────────────┬────────────────┘
          │                │                 │                  │
          ▼                ▼                 ▼                  ▼
   ┌────────────┐   ┌──────────────┐  ┌─────────────┐   ┌────────────────┐
   │ PostgreSQL │   │    Redis     │  │ Cloudflare  │   │   GitHub API   │
   │  users     │   │  queue  +    │  │     R2      │   │  repos, source │
   │  projects  │   │  pub/sub     │  │ built sites │   │  webhooks      │
   │  deployments│  └──────┬───────┘  └──────▲──────┘   └────────────────┘
   └────────────┘         │                  │
                          │  build jobs      │ upload (worker)
                          │  + status events │ read   (API serves)
                          ▼                  │
   ┌─────────────────────────────────────────────────────────────────┐
   │                  Build Worker  ·  Spring Boot  ·  GCP VM         │
   │                                                                  │
   │   BRPOP job  →  download repo  →  docker run node:20             │
   │              →  npm install && npm run build                     │
   │              →  upload dist/ to R2                               │
   │              →  publish status to Redis pub/sub                  │
   └─────────────────────────────────────────────────────────────────┘
```

**The same JAR runs in two roles.** A single Spring Boot build is deployed twice — as the API on Railway and as the worker on the GCP VM — switched by an environment flag (`WORKER_ENABLED`). It's a **modular monolith**, not microservices: shared code, separate runtime responsibilities.

| Component | Runs on | Responsibility |
|---|---|---|
| **API** | Railway | Auth, projects, deployments, serving built sites, SSE |
| **Build worker** | GCP VM | Consumes the queue, builds repos in Docker, uploads output |
| **PostgreSQL** | Railway | Users, projects, deployment history |
| **Redis** | Railway | Build queue (`LPUSH`/`BRPOP`) + status pub/sub |
| **Cloudflare R2** | Cloudflare | Object storage for every built site (S3-compatible) |

---

## How a deployment works

Every deployment travels the same path, whether triggered manually or by a git push:

```
 push to GitHub  ──webhook──┐         click "Deploy" ──┐
 (HMAC verified)            │                          │
                            └────────────┬─────────────┘
                                         ▼
 [API]  create Deployment (QUEUED)  ──LPUSH──▶  [ Redis queue ]
                                                      │  BRPOP (blocking)
                                                      ▼
 [Worker · GCP VM]
   1. mark BUILDING          ──publish──▶ Redis ──▶ [API] ──SSE──▶ browser (live)
   2. download repo source @ the target commit  (GitHub zipball)
   3. detect framework + root dir, then:
        docker run --rm --memory 2g node:20  sh -c "npm install && npm run build"
   4. locate output (dist / build / out)  →  upload to  R2: deployments/<id>/...
   5. mark READY + set as the project's live deployment
                             ──publish──▶ Redis ──▶ [API] ──SSE──▶ browser (live)

 visitor ──GET /sites/<subdomain>/──▶ [API] ── resolve current deployment
                                             ── read deployments/<id>/index.html from R2
                                             ── stream file back
```

**Immutable snapshots.** Each deployment's files are written to their own R2 prefix (`deployments/<id>/`) and never overwritten. That single decision is what makes **instant rollback**, **deployment history**, and **preview URLs** nearly free — going live with a version is just pointing the project's "current" pointer at a different snapshot; no rebuild.

---

## Real-time build status

The API holds the browser connections, but the **worker** (on a different machine) is what changes a build's status. Redis pub/sub bridges the two, and the API streams updates to the browser over **Server-Sent Events**:

```
 WORKER (GCP VM)            REDIS                    API (Railway)              BROWSER
 ──────────────            ─────                    ─────────────              ───────
 status → BUILDING ──PUBLISH──▶ "deployment-events" ──▶ SUBSCRIBE
                                                          │
                                                          ▼
                                              push to the SSE connections
                                              watching that deployment  ──▶  status flips
                                                                              live, no refresh
```

- Because **every** instance subscribes to the channel, this scales horizontally with no shared state — each instance forwards events to its own connections.
- A 20-second **heartbeat** keeps streams alive through proxy idle timeouts; disconnects are pruned automatically.

---

## Why a separate build machine

Building a user's project means running **their** code (`npm install` runs their scripts). Doing that on the API server would be slow and dangerous. So builds are isolated on their own VM and sandboxed in Docker:

```
docker run --rm
  --user "$(id -u):$(id -g)"      # output owned by the worker, not root
  --memory 2g                     # capped memory
  -v <repo>:/app  -w /app
  node:20  sh -c "npm install && npm run build"
# wrapped in `timeout` so a runaway build can't hang the worker
```

A failed or malicious build fails inside a throwaway container — the host and the platform are unaffected.

---

## Features

- **GitHub OAuth** login; access tokens **encrypted at rest** (AES-GCM)
- **Connect any repo** — framework auto-detection (Vite, CRA, Next, …) and a configurable **root directory** for monorepos
- **Real builds** in isolated Docker containers, on a dedicated VM
- **Auto-deploy on push** via **HMAC-verified GitHub webhooks**
- **Real-time status** (QUEUED → BUILDING → READY/FAILED) over SSE
- **Build logs** captured per deployment (see exactly why a build passed or failed)
- **Instant rollback / promote** — switch the live version with no rebuild
- **Deployment history**, **per-commit deploys**, and **per-deployment preview URLs**
- **Cloudflare R2** object storage; swappable local-disk backend for dev

---

## Tech stack

| Layer | Choice |
|---|---|
| Language / runtime | **Java 17** |
| Framework | **Spring Boot 4.1** (Spring MVC, Security, Data JPA, Data Redis) |
| Database | **PostgreSQL** + **Flyway** migrations |
| Queue & pub/sub | **Redis** (Lettuce) |
| Object storage | **Cloudflare R2** via AWS SDK v2 (S3-compatible) |
| Build sandbox | **Docker** (`node:20`) |
| Auth | GitHub OAuth · JWT (HS256) |
| Hosting | API on **Railway**, worker on a **GCP** Compute Engine VM |
| Frontend | **React + Vite** (separate repo) |

---

## Project structure

Layer-based packaging — each package is a responsibility:

```
com.aliahmed.Vercel
├── Controllers/     REST + SSE endpoints, public site serving
├── Services/        core logic: build pipeline, queue, storage, OAuth, framework detection
├── Repositories/    Spring Data JPA interfaces
├── entity/          JPA entities (User, Project, Deployment, GithubAccount, ...)
├── dto/             request/response records
├── mapper/          entity → DTO
├── config/          Security, CORS, Redis pub/sub, R2, typed properties
├── exception/       domain exceptions + global handler
└── util/            crypto, subdomain slugs, path validation
```

---

## Data model

```
User ───< Project ───< Deployment
│         │             │
│         │             ├─ status: QUEUED → BUILDING → READY | FAILED
│         │             ├─ commitSha   (the built commit)
│         │             └─ isCurrent   (one live deployment per project)
│         ├─ repoFullName, defaultBranch, subdomain
│         ├─ framework, rootDirectory, defaultPath   (build settings)
│         └─ githubHookId                            (auto-deploy webhook)
└─ GithubAccount (encrypted OAuth token)
```

A partial unique index enforces **at most one `isCurrent` deployment per project** — the invariant that makes rollback safe.

---

## API reference

All `/api/**` routes require `Authorization: Bearer <jwt>` unless noted.

**Auth**
```
GET  /api/auth/github/authorize        start GitHub OAuth        (public)
POST /api/auth/exchange                one-time code → JWT       (public)
GET  /api/auth/me                      current user
```

**Projects**
```
GET  /api/github/repos                 list the user's GitHub repos
POST /api/projects                     connect a repo (idempotent)
GET  /api/projects[?deployed=true]     list projects
GET  /api/projects/{id}                one project
GET  /api/projects/{id}/commits        repo commit history
PATCH /api/projects/{id}               update rootDirectory / defaultPath
DELETE /api/projects/{id}              disconnect
```

**Deployments**
```
POST /api/projects/{id}/deployments               deploy (optional {"commit":"<sha>"})
GET  /api/projects/{id}/deployments               history
GET  /api/projects/{id}/deployments/{dId}         one deployment
POST /api/projects/{id}/deployments/{dId}/promote rollback / go live (no rebuild)
GET  /api/projects/{id}/deployments/{dId}/logs    build logs (text)
GET  /api/projects/{id}/deployments/{dId}/events  live status stream (SSE)
```

**Public**
```
POST /api/webhooks/github              GitHub push events (HMAC-verified)
GET  /sites/{subdomain}/**             serve a project's live site
GET  /d/{deploymentId}/**              serve a specific deployment (preview URL)
```

---

## Security

- GitHub OAuth tokens **encrypted at rest** with AES-GCM
- Webhooks authenticated by **HMAC-SHA256 signature** (constant-time compare) — not a token
- User code built in **isolated, resource-limited** Docker containers, never on the host
- Path/traversal validation on user-supplied root directories and served paths
- Stateless JWT sessions; CORS locked to the configured frontend origin

---

## Running locally

**Requirements:** Java 17, Maven, Docker (for Node builds), and local or remote PostgreSQL + Redis.

```bash
# 1. configure environment (see .env.example)
export DB_URL=jdbc:postgresql://localhost:5432/nexel
export DB_USERNAME=postgres DB_PASSWORD=postgres
export REDIS_URL=redis://localhost:6379
export JWT_SECRET=$(openssl rand -base64 32)
export CRYPTO_SECRET=$(openssl rand -base64 32)
export GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=...
export STORAGE_TYPE=local            # or r2 (+ R2_* vars)

# 2. run the API (also runs the worker unless WORKER_ENABLED=false)
./mvnw spring-boot:run
```

Flyway applies the schema on startup. Set `WORKER_ENABLED=false` to run a pure API instance and start the worker separately (that's how it runs in production).

---

## Deployment

```
API  (Railway)                         Worker  (GCP Compute Engine VM)
─────────────                          ──────────────────────────────
• deploy from GitHub                   • git pull && ./mvnw -q -DskipTests package
• Postgres + Redis add-ons             • WORKER_ENABLED=true
• STORAGE_TYPE=r2  (+ R2_* creds)      • Docker installed (runs node:20 builds)
• WEBHOOK_SECRET for auto-deploy       • reaches Redis/Postgres via public networking
```

The worker connects to Railway's **public** Postgres/Redis endpoints (it lives off-network on GCP); the API uses the internal ones.

---

## Roadmap

- Per-site subdomains via wildcard DNS (host-based routing) — serve each site at its own root
- Live-streaming build logs (line by line) over the existing SSE channel
- Per-project environment variables (encrypted)
- Reliable-queue semantics so a crashed worker never loses a build

---

## Credits

- **Backend & platform** — the build pipeline, distributed worker, storage, serving, real-time, and API.
- **Frontend** — React + Vite dashboard, built by my teammate.

Built as a deep dive into how modern deployment platforms actually work.
