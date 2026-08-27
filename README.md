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

### Backend (H2)
1. From repo root: `./gradlew :server:run`
2. DB file appears under `server/build/db/kejasafe*`

### Client
- **Android**: `./gradlew :app:androidApp:installDebug`
- **iOS**: Open `app/iosApp/iosApp.xcworkspace` in Xcode.
- **Web (Wasm)**: `./gradlew :app:webApp:wasmJsBrowserRun`

### Firebase Data Connect / Cloud SQL (paused)
Schema + connector live in `server/dataconnect/`. Not required for day-to-day H2 development.
When you are ready to pay for Cloud SQL, see [`server/dataconnect/README.md`](server/dataconnect/README.md).

*Built for Kenya, by Engineers who value Sanctuary.*

