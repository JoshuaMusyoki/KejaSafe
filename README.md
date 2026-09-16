# KejaSafe: The Reliable Sanctuary for Kenyan Housing

KejaSafe is a comprehensive housing ecosystem built for the Kenyan market, designed to bridge the trust gap between students, tenants, and landlords. Our mission is to provide a "reliable sanctuary" by automating compliance, payments, and legal requirements according to Kenyan Law.

---

##  Engineering Architecture

KejaSafe is built with a high-performance, scalable foundation designed to minimize build times, reduce app size, and ensure cross-platform consistency.

### 1. Feature-Based Modularization
The project is strictly divided into independent feature modules (e.g., `:feature:auth`, `:feature:rent`, `:feature:discovery`).
- **Isolation**: Changes in one feature do not trigger recompilation of others.
- **Scalability**: New features can be developed and tested in parallel.
- **Dynamic Delivery**: Optimizes the app size by only including necessary code in specific platform builds.

### 2. Gradle Convention Plugins (`:build-logic`)
To maintain build health, we use **Convention Plugins** to standardize configurations:
- `KejaSafe.android.library`: Configures Android-specific library settings.
- `KejaSafe.compose.multiplatform`: Standardizes Compose and Material3 setups.
- `KejaSafe.kotlin.multiplatform`: Manages KMP target configurations (Android, iOS, JVM, Wasm).
- **Benefit**: Reduces thousands of lines of boilerplate and ensures 100% configuration consistency.

### 3. Shared Multiplatform Logic (KMP)
- **UI**: Shared Compose Multiplatform components in `:core:ui`.
- **Business Logic**: Shared ViewModels and Repositories in `:app:shared`.
- **Domain**: Shared models and validation rules in `:core:domain`.

---

## 🚀 Product Vision & Features

Designed with a deep understanding of the **Kenyan Landlord and Tenant Act**, KejaSafe offers an inclusive service for both students and property owners.

### 🏢 For Landlords: Total Property Management
- **eTIMS Invoice Generation**: Automatically generate and send KRA-compliant eTIMS invoices for every rent payment.
- **KRA Rental Income Tax Filing**: Simplified calculation and automated filing assistance for Monthly Rental Income (MRI) tax (10%).
- **Digital Lease Management**: Legally binding lease agreements based on Kenyan law, with e-signatures.
- **KYC & Tenant Screening**: Integrated ID/Passport verification and background checks.
- **Maintenance Tracking**: Log repair requests and track contractor costs for tax deductions.
- **Vacancy Analytics**: Real-time insights into occupancy rates and rental yields.

### 🎓 For Students & Tenants: Secure Living
- **M-Pesa Rent Payments**: Seamless integration for direct-to-landlord payments with instant receipts.
- **Automated Utility Billing**: Transparent calculation of water and electricity bills (including KPLC token management).
- **Rent Financing/Split-Pay**: Options for students to split rent with roommates securely.
- **Verified Listings**: Every property on KejaSafe is physically verified to prevent "rent scams."
- **In-App Dispute Resolution**: Evidence logging for the Rent Restriction Tribunal or Environment and Land Court.

---

## 🛠️ Technology Stack

- **Frontend**: Kotlin Multiplatform, Compose Multiplatform, Navigation 3.
- **Backend**: Ktor (Kotlin), Exposed DAO/DSL (`:server`).
- **Database (now)**: **H2** file DB for local/dev (`jdbc:h2:file:./build/db/kejasafe`).
- **Database (later)**: Firebase Data Connect → Cloud SQL — schema in [`server/dataconnect/`](server/dataconnect/).
- **Identity**: Firebase Auth (email/password) + Google Identity Services.
- **Payments**: Safaricom M-Pesa Daraja API.
- **Invoicing**: eTIMS V2 API.

---

## 🚀 Development Setup

### Prerequisite: JDK 21

The `:core` module compiles to Java 21 bytecode, so an older JDK fails at runtime with
`UnsupportedClassVersionError`. Point `JAVA_HOME` at a JDK 21 before anything else:

```powershell
# Windows / PowerShell — set once, then reopen the terminal
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\path\to\jdk-21", "User")
```

```bash
# macOS / Linux
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Running the full stack

The backend, web app and mobile app each hold a terminal, and they are designed to run at the
same time. Start the backend first — the clients expect it on port `8080`.

**Terminal 1 — backend APIs**

```bash
./gradlew :server:run
```

Wait for `Responding at http://0.0.0.0:8080`, then check `curl http://localhost:8080/health`.
The H2 file DB appears under `server/build/db/kejasafe*` and survives restarts.
Interactive API docs: **http://localhost:8080/swagger**.

**Terminal 2 — web app (Wasm)**

```bash
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun --continuous
```

Opens on **http://localhost:3000** (the JS target uses `3001`). `--continuous` rebuilds and
reloads the browser as you edit Compose code. The browser derives the API address from the page
host on port `8080`, so `localhost:3000` talks to `localhost:8080` with no extra configuration.

**Terminal 3 — mobile**

```bash
./gradlew :app:androidApp:installDebug   # Android device or emulator
```

iOS needs macOS and Xcode: `cd app/iosApp && pod install`, then open the generated
`iosApp.xcworkspace` and run.

Mobile builds compile the API address in, so set `ApiConfig.DEV_API_BASE_URL` in
[`core/src/commonMain/kotlin/com/rotd/kejasafe/network/ApiConfig.kt`](core/src/commonMain/kotlin/com/rotd/kejasafe/network/ApiConfig.kt)
to reach your machine:

| Target | Base URL |
| --- | --- |
| Physical device on the same Wi-Fi | `http://<your-LAN-IP>:8080` (`ipconfig` / `ifconfig`) |
| Android emulator | `http://10.0.2.2:8080` |
| iOS simulator | `http://localhost:8080` |

The server binds `0.0.0.0`, so a physical device only needs your firewall to allow inbound 8080.
Cleartext HTTP is already enabled in the Android manifest for local development.

### Ports and overrides

| Port | Service |
| --- | --- |
| `8080` | Ktor API and Swagger UI |
| `3000` | Wasm web dev server |
| `3001` | JS web dev server |

- Move the API: `PORT=9090 ./gradlew :server:run` (`$env:PORT="9090"` first, on PowerShell). The web app then needs
  `window.KEJASAFE_API_BASE_URL` set in [`app/webApp/src/webMain/resources/index.html`](app/webApp/src/webMain/resources/index.html).
- Restrict browser origins (required for deployments, optional locally):
  `KEJASAFE_ALLOWED_ORIGINS=https://kejasafe.co.ke`.
- Other overrides live in [`server/src/main/kotlin/com/rotd/kejasafe/config/ServerConfig.kt`](server/src/main/kotlin/com/rotd/kejasafe/config/ServerConfig.kt):
  `KEJASAFE_JWT_SECRET`, `KEJASAFE_DB_URL`, `KEJASAFE_UPLOAD_DIR`.
- A leftover JVM keeps port 8080 and the H2 file lock, which blocks the next start. On Windows:
  `Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080 -State Listen).OwningProcess -Force`.

### Deploying

The API runs on Cloud Run with Cloud SQL and a Cloud Storage bucket, and the web app is a
static bundle on Firebase Hosting at [kejasafe.web.app](https://kejasafe.web.app).

GitHub Actions covers the rest: [`ci.yml`](.github/workflows/ci.yml) tests every pull
request and builds the APK, jar and container image, while
[`deploy-web.yml`](.github/workflows/deploy-web.yml) and
[`deploy-api.yml`](.github/workflows/deploy-api.yml) release the web app and the API on
pushes to `master_dev`. The secrets and variables they need are listed in
[`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

See [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) for the full release. Every script takes
`-DryRun`, which prints the commands without calling Google Cloud or spending anything:

```powershell
Copy-Item deploy/release.config.ps1.example deploy/release.config.ps1
./deploy/deploy-api.ps1 -Setup -DryRun
./deploy/deploy-web.ps1 -DryRun
```

### Backend in Docker

The image expects a fat jar built on the host, because `:core` needs the Android SDK to compile:

```bash
./gradlew :server:buildFatJar
docker compose up --build
```

Same URLs as the Gradle workflow. Uploads and the database persist in the `kejasafe-data` volume.

### Seeding content

Listings and community stories come from real accounts, so a fresh database shows empty states and
zeroed counters. Register a landlord and publish a listing, or sign in as a student and post from
the composer on the landing page. Stories must be 40–600 characters and only `STUDENT` or
`LANDLORD` accounts may post.

### Firebase Data Connect / Cloud SQL (paused)
Schema + connector live in `server/dataconnect/`. Not required for day-to-day H2 development.
When you are ready to pay for Cloud SQL, see [`server/dataconnect/README.md`](server/dataconnect/README.md).

*Built for Kenya, by Engineers who value Sanctuary.*

