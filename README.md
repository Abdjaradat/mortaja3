# رائد (Ra'ed)

A marketplace platform connecting Jordanian military officers (rank Major / رائد and above) who hold a one-time vehicle tax exemption with civilian buyers.

> **This app is a discovery and matching platform only.** It does not facilitate transactions, hold funds, or act as a party to any contract.

---

## Monorepo Structure

```
Mortaja3/
├── android/        # Android app (Kotlin + Jetpack Compose)
└── backend/        # Node.js REST API (TypeScript + Express + Prisma)
```

---

## Running the Backend

### Prerequisites
- Node.js 20+
- PostgreSQL 15+

### Setup

```bash
cd backend

# 1. Install dependencies
npm install

# 2. Copy and fill in environment variables
cp .env.example .env
# Edit .env: set DATABASE_URL, JWT_SECRET, Firebase credentials

# 3. Generate Prisma client
npm run db:generate

# 4. Run migrations (requires live DB)
npm run db:migrate

# 5. Start dev server (hot-reload)
npm run dev
```

The API will be available at `http://localhost:3000`.

Health check: `GET http://localhost:3000/health`

### Key scripts

| Script | Action |
|---|---|
| `npm run dev` | Start with nodemon hot-reload |
| `npm run build` | Compile TypeScript → dist/ |
| `npm start` | Run compiled dist/server.js |
| `npm run db:migrate` | Create and apply a new migration |
| `npm run db:studio` | Open Prisma Studio (DB GUI) |

---

## Running the Android App

### Prerequisites
- Android Studio Ladybug (2024.2) or newer
- JDK 17+
- Android SDK with API 35

### Setup

1. Open `android/` as a project in Android Studio
2. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable **Phone Authentication**
   - Enable **Cloud Storage**
   - Enable **Cloud Messaging**
   - Download `google-services.json` → place in `android/app/`
3. In `android/app/src/main/res/font/` add the Cairo font files:
   - `cairo_regular.ttf`
   - `cairo_medium.ttf`
   - `cairo_semibold.ttf`
   - `cairo_bold.ttf`
   
   Download from [Google Fonts — Cairo](https://fonts.google.com/specimen/Cairo)
4. Sync Gradle and run on an emulator or device (API 24+)

### Min SDK
Android 7.0 (API 24)

---

## Environment Variables (Backend)

| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL connection string |
| `JWT_SECRET` | Random 256-bit secret for JWT signing |
| `JWT_EXPIRES_IN` | Access token lifetime (e.g. `7d`) |
| `JWT_REFRESH_EXPIRES_IN` | Refresh token lifetime (e.g. `30d`) |
| `PORT` | Server port (default: 3000) |
| `NODE_ENV` | `development` or `production` |
| `FIREBASE_PROJECT_ID` | Firebase project ID |
| `FIREBASE_PRIVATE_KEY` | Firebase Admin SDK private key |
| `FIREBASE_CLIENT_EMAIL` | Firebase Admin SDK client email |
| `DOCUMENT_ENCRYPTION_KEY` | 32-byte hex key for encrypting document URLs at rest |
| `CORS_ORIGIN` | Allowed CORS origin |

---

## API Overview (`/api/v1`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/request-otp` | — | Request phone OTP |
| POST | `/auth/verify-otp` | — | Verify OTP, get JWT |
| POST | `/auth/refresh` | — | Refresh access token |
| GET | `/users/me` | Bearer | Get own profile |
| PATCH | `/users/me` | Bearer | Update profile |
| DELETE | `/users/me` | Bearer | Schedule account deletion |
| POST | `/users/me/officer-profile` | Officer | Submit verification docs |
| GET | `/listings` | — | Browse listings (filterable) |
| POST | `/listings` | Verified Officer | Create listing |
| GET | `/conversations` | Bearer | List conversations |
| POST | `/conversations/:id/messages` | Bearer | Send message |
| POST | `/reports` | Bearer | Report user/listing/message |
| GET | `/admin/pending-verifications` | Admin | Review officer submissions |

---

## Sprint Plan

| Sprint | Week | Focus |
|---|---|---|
| 1 | 1 | Foundations — project setup, theme, navigation skeleton |
| 2 | 2 | Auth — phone OTP end-to-end, JWT session |
| 3 | 3 | Officer verification — doc upload, admin review page |
| 4 | 4 | Listings — create, browse, filter, save |
| 5 | 5 | Chat — real-time via Socket.io, FCM push |
| 6 | 6 | Calculator & resources — static content, PDF |
| 7 | 7 | Polish — reports, deletion, accessibility, Crashlytics |
| 8 | 8 | Pre-launch — privacy policy, Play Store assets, beta |

---

## Security Notes

- Military ID documents are stored in Firebase Storage with signed, time-limited URLs
- Sensitive document references are additionally encrypted at the application layer (`DOCUMENT_ENCRYPTION_KEY`)
- Phone numbers are never logged
- All endpoints are rate-limited per IP and per user
- TLS 1.2+ enforced; HSTS header set
- Account deletion wipes all PII within 30 days (Play Store compliance)

---

## Legal Disclaimers (required in app)

**Welcome screen:**
> رائد منصة تعارف بين الأطراف فقط. نحن لا نشتري أو نبيع المركبات، ولسنا طرفًا في أي اتفاق. جميع المعاملات مسؤولية المستخدمين.

**Before posting a listing:**
> بنشرك هذا الإعلان، تقرّ بأنك تمتلك حق الإعفاء وأنك لم تستخدمه سابقًا. أي تصريح كاذب مسؤوليتك القانونية الشخصية.

**Inside every chat:**
> ينصح باستشارة محامٍ قبل توقيع أي عقد. للاطلاع على نموذج عقد مراجَع من محامٍ، افتح قسم الموارد.
