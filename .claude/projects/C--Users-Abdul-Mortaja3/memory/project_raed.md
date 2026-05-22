---
name: project-raed
description: Ra'ed (رائد) — Jordanian military vehicle tax-exemption marketplace. Android + Node.js backend monorepo at C:\Users\Abdul\Mortaja3
metadata:
  type: project
---

Ra'ed is a two-sided marketplace app connecting Jordanian military officers (rank Major/رائد and above, active or retired) who hold a one-time vehicle tax exemption right with civilian buyers.

**Why:** December 2025 Jordanian government approved tax exemptions for eligible officers. A secondary market exists; Ra'ed formalizes it with verification and discovery tools.

**How to apply:** When working on this project, keep the military/legal context in mind. The app is a matching platform ONLY — never suggest features that facilitate transactions, hold funds, or make the app a party to contracts.

## Stack
- Backend: `C:\Users\Abdul\Mortaja3\backend` — Node.js + TypeScript + Express + Prisma 7 + PostgreSQL + Socket.io
- Android: `C:\Users\Abdul\Mortaja3\android` — Kotlin + Jetpack Compose + Material 3 + Hilt + Retrofit + Firebase
- Prisma 7 note: datasource URL is in `prisma.config.ts` (not schema.prisma) — this is a Prisma 7 breaking change

## Sprint status (as of 2026-05-19)
- Sprint 1 (foundations): **COMPLETE** — skeleton scaffolded, TypeScript compiles clean, Prisma schema validates
- Sprint 2 (auth): **COMPLETE** — Firebase phone OTP client flow, backend Firebase ID token verification, JWT session, DataStore token storage, 4 auth screens (PhoneInput → OtpVerify → UserType → ProfileSetup), SplashViewModel startup routing
- Sprint 3: Officer verification — TODO
- Sprint 4: Listings — TODO
- Sprint 5: Chat — TODO
- Sprint 6: Calculator & resources — TODO
- Sprint 7: Polish — TODO
- Sprint 8: Pre-launch — TODO

## Critical requirements
- RTL-first (Arabic primary, English secondary)
- Military ID documents: encrypted storage, signed time-limited URLs
- Legal disclaimers must appear verbatim at welcome, listing creation, and in every chat
- Account deletion must wipe PII within 30 days (Play Store compliance)
- No logging of PII (phone numbers, names, document URLs)
