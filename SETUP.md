# BidForge — Setup & Run

Two parts: the Spring Boot API (`bidforge/`, port 8080) and the Angular app (`bidforge-angular/`, port 4200). Sections
1–7 cover the backend, section 8 the frontend.

## Prerequisites

- JDK 21+
- Oracle database (Docker or existing)
- Docker (for the Docker setup and integration tests)
- Node.js 20.19+ / 22.12+ (frontend only)

---

## 1. Secrets (always required)

The app has no default password or JWT key and refuses to start without them.

```bash
cp .env.example .env
```

Set in `.env`:

```properties
DB_PASSWORD=pick_a_password
ORACLE_PASSWORD=pick_another_password
JWT_SECRET=at-least-32-characters-of-random-text
```

Generate a key: `openssl rand -base64 48`

---

## 2a. Run with Docker

```bash
docker compose --env-file .env -f docs/docker-compose.yml up -d
cd bidforge && ./mvnw spring-boot:run
```

The database is created and `schema.sql` applied automatically.

API -> http://localhost:8080
Swagger -> http://localhost:8080/swagger-ui.html

**Stop / reset:**

```bash
docker compose --env-file .env -f docs/docker-compose.yml down      
docker compose --env-file .env -f docs/docker-compose.yml down -v 
```

---

## 2b. Run without Docker

**Create the schema user** (as `SYSTEM` or `SYS AS SYSDBA`):

```sql
-- ALTER SESSION SET CONTAINER = FREEPDB1
CREATE USER bidforge IDENTIFIED BY "your_password";
GRANT CONNECT, RESOURCE TO bidforge;
ALTER USER bidforge QUOTA UNLIMITED ON USERS;
```

**Create the tables** (the app never creates them, it runs `ddl-auto=validate`):

```bash
sqlplus bidforge/your_password@//localhost:1521/FREEPDB1 \
        @bidforge/src/main/resources/db/schema.sql
```

Or open `bidforge/src/main/resources/db/schema.sql` in any SQL client and run it as `bidforge`.

**Point the app at it**, add to `.env`:

```properties
DB_URL=jdbc:oracle:thin:@//your-host:1521/YOUR_SERVICE
DB_USER=bidforge
DB_PASSWORD=your_password
```

**Run:**

```bash
cd bidforge && ./mvnw spring-boot:run
```

---

## 3. Profiles

| Profile         | Demo data | Swagger | SQL logs |
|-----------------|-----------|---------|----------|
| `dev` (default) | yes       | yes     | yes      |
| `prod`          | **no**    | no      | no       |
| `test`          | no        | –       | no       |

```bash
./mvnw spring-boot:run                                    # dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod    # prod
java -jar target/bidforge-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 4. Tests

```bash
cd bidforge
./mvnw test      # 44 unit tests, no Docker
./mvnw verify    # 55 tests total, adds integration tests, needs Docker
```

```bash
./mvnw test -Dtest=PagingTest                  # single unit class
./mvnw verify -Dit.test=BiddingConcurrencyIT   # single integration class
./mvnw clean package                           # build jar (runs unit tests)
```

---

## 5. Postman

Import `docs/BidForge.postman_collection.json`, run folder **1 Auth** first (it stores the JWTs), then any folder.

```bash
npx newman run docs/BidForge.postman_collection.json   # 53 requests, 82 assertions
```

---

## 6. Demo accounts (dev profile only)

| Username  | Password       | Role  | Used in the sample data as                        |
|-----------|----------------|-------|---------------------------------------------------|
| `admin`   | `Admin@123`    | ADMIN | Administrator                                     |
| `sara`    | `Password@123` | USER  | Seller                                            |
| `omar`    | `Password@123` | USER  | Seller                                            |
| `layla`   | `Password@123` | USER  | Bidder (winning, outbid, sealed and won auctions) |
| `youssef` | `Password@123` | USER  | Bidder                                            |

Seven sample auctions are seeded: one scheduled, three open, two closed with winners, one cancelled.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"layla","password":"Password@123"}'
```

---

## 7. Environment variables

| Variable                 | Required    | Default                                     |
|--------------------------|-------------|---------------------------------------------|
| `DB_PASSWORD`            | **yes**     | —                                           |
| `JWT_SECRET`             | **yes**     | — (min 32 chars)                            |
| `ORACLE_PASSWORD`        | Docker only | —                                           |
| `DB_URL`                 | no          | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| `DB_USER`                | no          | `bidforge`                                  |
| `JWT_EXPIRATION_MS`      | no          | `86400000`                                  |
| `CORS_ALLOWED_ORIGINS`   | no          | `http://localhost:4200`                     |
| `SPRING_PROFILES_ACTIVE` | no          | `dev`                                       |

---

## 8. Angular frontend

Start the backend first (sections 1–2) — the app calls it at `http://localhost:8080/api`.

```bash
cd bidforge-angular
npm install
npm start
```

App -> http://localhost:4200

Sign in with the demo accounts from section 6; the login page lists them as one-click buttons.

**Production build:**

```bash
npm run build     # output in dist/bidforge-angular/browser
```

The two run as separate servers and talk over CORS, which the backend already allows for
`http://localhost:4200`. To use a different API address, edit `apiBaseUrl` in
`src/environments/environment.development.ts`.

More detail — screen map, architecture, troubleshooting — in `bidforge-angular/README.md`.

---

