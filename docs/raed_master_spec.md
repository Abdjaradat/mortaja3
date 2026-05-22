# رائد (Ra'ed) — Master Specification v2.0

> **The Single Source of Truth.** This document supersedes all previous specs.
> Pass this entire file to Claude Code as the project's authoritative reference.

---

## 1. Vision & Positioning

**Ra'ed** is an Android-first Jordanian marketplace built on two complementary engines:

1. **Exemption Marketplace (سوق الإعفاءات)** — The flagship feature. Connects 
   military officers (Major and above) holding one-time vehicle tax exemption rights 
   with civilian buyers and licensed brokers, with built-in trust and circumvention 
   protection.

2. **Car Marketplace (سوق السيارات)** — A general used-car classifieds platform with 
   map-based discovery, monetized through ad tiers and dealer packages (like OpenSooq, 
   but with map-first UX and exemption integration).

The two marketplaces share users, accounts, and authentication, but have different 
monetization models, different flows, and partially different UI.

---

## 2. User Types

| Type | Arabic | Description | Can Do |
|------|--------|-------------|--------|
| BUYER | مشتري | General user looking to buy | Browse both marketplaces, contact sellers, post car listings |
| OFFICER | ضابط | Verified military officer (Major+) | Post exemption listings, browse car marketplace |
| BROKER | وسيط | Verified exemption broker | Reserve leads, manage deals, post car listings, access broker tools |
| DEALER | معرض | Verified car dealership | Bulk car listings, dealer page, analytics |
| ADMIN | مدير | Platform admin | Verify users, moderate, manage disputes |

A single user account can hold multiple roles (e.g., an officer can also be a buyer).

---

## 3. Marketplace 1: Exemption Marketplace

### 3.1 Core Concept

Officers post exemption rights. Buyers seek exempted vehicles. Brokers facilitate.
Identity is **blacked out** until commission is paid — this is the platform's 
anti-circumvention guarantee.

### 3.2 The Three-Sided Flow

```
[Buyer] posts a need → [Platform] matches to Brokers → [Broker] reserves lead
   ↓                                                          ↓
   |                                                  Blind chat begins
   |                                                          ↓
   |                                                  Agreement on deal
   |                                                          ↓
   |←—————— Both parties pay reveal fees ————→ Identity revealed
                                                              ↓
                                                   Offline transaction
                                                              ↓
                                                   Platform tracks closure
```

### 3.3 Monetization (Exemption Side)

| Fee | Amount (JOD) | Who Pays | When | Purpose |
|-----|--------------|----------|------|---------|
| Lead reservation | 2 | Broker | On clicking "I'm interested" | Prevents lead hoarding, signals seriousness |
| Account activity fee | 5/month | Broker | Auto-monthly, if active that month | Keeps account active, basic revenue |
| Commission (tiered) | 75–250 | Broker | When both parties click "Agree" | Reveal fee, main commission |
| Buyer reveal fee | 25 | Buyer | When both parties click "Agree" | Filters non-serious buyers |
| Deal tracking fee | 1/day | Broker | After reveal, until closure | Encourages quick closure |

**Commission tiers (by agreed vehicle price):**
- < 20,000 JOD → 75 JOD
- 20,000–35,000 JOD → 125 JOD
- 35,000–50,000 JOD → 175 JOD
- > 50,000 JOD → 250 JOD

**Future Phase 3:** transition to 3–5% on platform-matched leads.

### 3.4 Wallet System (mandatory for brokers)

All small fees deducted from broker's in-app wallet. Wallet topped up via HyperPay 
or Madfoatcom in larger amounts to amortize gateway fees.

**Top-up bonuses:**
- 50 JOD → +5 JOD bonus
- 100 JOD → +12 JOD bonus
- 200 JOD → +30 JOD bonus

**Refund rules:**
- Lead reservation: 50% refund if buyer doesn't respond within 48h
- Reveal commission: non-refundable once paid
- Tracking fee: stops auto-deducting when deal marked closed

### 3.5 Identity Blackout Rules

Until both parties pay reveal fees:
- Buyer sees: broker's first name only, rating, deal count, governorate, 
  years active, "Verified" badge
- Broker sees: buyer's first name, governorate, budget range, vehicle preferences

After reveal:
- Full names, phone numbers, exact location, in-app calling unlocked

### 3.6 Anti-Circumvention Layer

**Message filtering (mandatory):**
- Regex filter for phone numbers, social handles, URLs
- AI moderation (Anthropic API) for disguised contact attempts   (numbers written as words, spaced characters, code phrases like "اتصل علي")
- Auto-block message + warning to sender
- 3 violations → 7-day suspension; 5 violations → permanent ban
- All flagged attempts logged for admin review

**Deal tracking:**
- 30 days after reveal: auto-prompt buyer "Did the deal proceed?"
- 60 days: second prompt
- 90 days: deal auto-closes; if buyer reports completion without app, broker flagged
- Pattern detection: brokers with high "ghosting" rates after reveal get audited

**Positive incentives (more effective than penalties):**
- 10 clean completed deals → "Trusted Broker" badge + commission tier discount (-20%)
- 25 deals → "Gold Broker" + priority in matching algorithm
- 50 deals → "Platinum Broker" + reduced tracking fee

### 3.7 In-App Anonymous Voice Calls

Built on LiveKit (leverage existing SwiftCall expertise). Allows buyer-broker 
voice calls without phone number exposure during the blackout phase.

- Free for first 5 minutes per deal
- 0.5 JOD per minute after (deducted from broker wallet, since they benefit more)
- Recorded for moderation (with disclosure to both parties)

### 3.8 Officer Listings (Direct, No Broker)

Officers can also list directly without a broker, for buyers who want to skip 
the middleman:

- Officer creates "exemption availability" post
- Buyers contact officer directly (with same blind chat rules)
- Direct fee: 50 JOD (one-time, paid by buyer to reveal officer contact)
- No broker commission, no wallet required for officer
- Officer gets 30 JOD of the fee, platform keeps 20 JOD

---

## 4. Marketplace 2: Car Marketplace

### 4.1 Core Concept

A full classifieds platform for used and new cars, **map-first**, with all 
standard features (search, filter, contact) but designed to outshine OpenSooq 
through geographic discovery and integrated services.

### 4.2 Listing Tiers

| Tier | Price (JOD) | Duration | Features |
|------|-------------|----------|----------|
| FREE | 0 | 30 days | 5 photos, basic info, normal feed position, 3/month per user |
| FEATURED (مميز) | 5 | 7 days | Top of search, 10 photos, "Featured" badge, home page rotation |
| VIP | 12 | 14 days | All Featured + auto-repost every 3 days, video, category top |
| GOLD (ذهبي) | 25 | 30 days | All VIP + push notifications to interested buyers, analytics dashboard |

### 4.3 Dealer Packages

For car dealerships (معارض السيارات):

| Package | Monthly (JOD) | Listings | Featured Slots | Extra |
|---------|---------------|----------|----------------|-------|
| Small (صغير) | 50 | 20 | 5 | Custom dealer page, logo |
| Medium (متوسط) | 120 | 50 | 15 | All Small + verified badge + analytics |
| Large (كبير) | 250 | Unlimited | 30 | All Medium + "Featured Dealers" section + bulk upload API |

### 4.4 Map-Based Discovery (Differentiating Feature)

**Default view:**
- Google Maps centered on user's location (with permission) or Amman
- Markers showing price per car
- Color-coded by body type (sedan/SUV/hatchback/pickup)
- Auto-clustering when zoomed out

**Interactions:**
- Tap marker → bottom sheet with car details
- Pinch/zoom to refine area
- Draw search area with finger → save as custom search
- Toggle list/map view
- "Cars in my area" → 5/10/20 km radius

**Advanced features:**
- Price heatmap layer (red = expensive area, green = affordable)
- Saved map searches with new-listing notifications
- Privacy modes: exact location | approximate (500m circle) | city only

### 4.5 Search & Filters

**Filters:**
- Body type, make, model, year range, price range
- Mileage, transmission, fuel type, color, condition
- Governorate, distance from user
- Has photos/video, dealer/private, negotiable
- Posted within (24h / week / month)

**Search bar:**
- Free text with smart autocomplete (make + model + year combinations)
- Voice search support (Arabic)
- Search by license plate prefix (for shoppers comparing similar plates)

**Sort options:**
- Newest first (default)
- Price low to high / high to low
- Mileage low to high
- Closest to me
- Best match (algorithmic, considers saved preferences)

### 4.6 Listing Creation Flow

**Step 1:** Vehicle details (make/model autocomplete from pre-loaded catalog)
**Step 2:** Specs (year, mileage, color, transmission, fuel type)
**Step 3:** Condition + description
**Step 4:** Photos (drag-reorder, up to 10/20 by tier)
**Step 5:** Location (auto-detect with permission, or pin on map, or city dropdown)
**Step 6:** Price + negotiable toggle
**Step 7:** Choose tier (FREE or paid) → wallet/payment if paid
**Step 8:** Review + publish

**Auto-features:**
- Image quality check (reject blurry/dark images with prompt)
- Duplicate detection (warn if same VIN or very similar specs from same user)
- Price suggestion based on similar listings in the database

### 4.7 Contact Methods

Car marketplace uses standard contact (no blackout, unlike exemption side):
- In-app chat (primary)
- Reveal phone number (one-tap, but counted in analytics)
- Voice call (for GOLD/dealer listings, via LiveKit)
- Save listing for later

### 4.8 Additional Revenue (Car Side)

- **Banner ads** for insurance, financing, maintenance companies (100–500 JOD/month)
- **Vehicle inspection partnership** (with mechanics network) — 20 JOD service, 10% to platform
- **Insurance partnership** — 5–10% commission on policies sold
- **Financing partnership** with banks — lead generation fee
- **Transfer assistance service** — 15 JOD per assisted transfer

---

## 5. Tech Stack

### Backend
- **Runtime:** Node.js 20 LTS + TypeScript
- **Framework:** Express (or Fastify if performance becomes critical)
- **ORM:** Prisma 7
- **Database:** PostgreSQL 16 + PostGIS extension (for map queries)
- **Cache:** Redis (for hot data: featured listings, broker matching)
- **Auth:** Firebase Auth (phone OTP) + custom JWT sessions
- **File Storage:** Firebase Storage (with strict access rules)
- **Real-time:** Socket.io for chat
- **Push:** Firebase Cloud Messaging (FCM)
- **Payments:** HyperPay (primary), Madfoatcom (fallback/alternative)
- **AI Moderation:** Anthropic API (Claude Sonnet 4)
- **Voice Calls:** LiveKit
- **Hosting:** DigitalOcean droplet or Hetzner (with managed Postgres)
- **CDN:** Cloudflare for static assets and image delivery

### Android
- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Min SDK:** 24 (Android 7.0)
- **Architecture:** MVVM + Repository, Hilt for DI
- **Networking:** Retrofit + OkHttp
- **Storage:** DataStore (preferences), Room (offline cache)
- **Maps:** Google Maps Compose
- **Images:** Coil
- **Charts:** Vico (for analytics)
- **Localization:** Arabic (primary, RTL), English (secondary)
- **Fonts:** Cairo (already installed)

### Admin Panel
- **Framework:** Next.js 14 (App Router) + Tailwind + shadcn/ui
- **Auth:** Same backend, admin role
- **Hosting:** Vercel (free tier sufficient)

---

## 6. Database Schema (Prisma)

```prisma
// ============ USERS ============

model User {
  id              String   @id @default(cuid())
  phoneNumber     String   @unique
  fullName        String?
  displayName     String?  // first name shown in blind chat
  governorate     String?
  city            String?
  photoUrl        String?
  bio             String?
  roles           UserRole[] // can have multiple: BUYER, OFFICER, BROKER, DEALER
  createdAt       DateTime @default(now())
  updatedAt       DateTime @updatedAt
  lastActiveAt    DateTime @default(now())
  isActive        Boolean  @default(true)
  isBlocked       Boolean  @default(false)
  blockReason     String?

  officerProfile  OfficerProfile?
  brokerProfile   BrokerProfile?
  dealerProfile   DealerProfile?
  wallet          Wallet?

  exemptionListings ExemptionListing[]
  carListings     CarListing[]
  buyerLeads      Lead[]            @relation("leadBuyer")
  brokerLeads     LeadReservation[]
  buyerDeals      Deal[]            @relation("dealBuyer")
  brokerDeals     Deal[]            @relation("dealBroker")
  officerDeals    Deal[]            @relation("dealOfficer")

  sentMessages    Message[] @relation("sender")
  conversations1  Conversation[] @relation("user1")
  conversations2  Conversation[] @relation("user2")
  savedListings   SavedListing[]
  reports         Report[] @relation("reporter")
  reportsAgainst  Report[] @relation("reported")
  ratingsGiven    Rating[] @relation("rater")
  ratingsReceived Rating[] @relation("rated")
  notifications   Notification[]
}

enum UserRole {
  BUYER
  OFFICER
  BROKER
  DEALER
  ADMIN
}

// ============ PROFILES ============

model OfficerProfile {
  id                String              @id @default(cuid())
  userId            String              @unique
  user              User                @relation(fields: [userId], references: [id])
  rank              String              // رائد، مقدم، عقيد، إلخ
  status            OfficerStatus       // ACTIVE | RETIRED
  branch            String              // قوات مسلحة، أمن عام، درك، إلخ
  documentUrl       String              // military ID or retirement doc (encrypted ref)
  verificationState VerificationState   @default(PENDING)
  verifiedAt        DateTime?
  verifiedBy        String?             // admin userId
  rejectionReason   String?
  exemptionUsed     Boolean             @default(false)
  exemptionUsedAt   DateTime?
  createdAt         DateTime            @default(now())
}

model BrokerProfile {
  id                String              @id @default(cuid())
  userId            String              @unique
  user              User                @relation(fields: [userId], references: [id])
  businessName      String?
  yearsActive       Int                 @default(0)
  specializations   String[]            // e.g., ["sedan", "SUV", "hybrid"]
  serviceAreas      String[]            // governorates served
  bio               String?             @db.Text
  documentUrl       String              // commercial registration or proof
  verificationState VerificationState   @default(PENDING)
  verifiedAt        DateTime?
  totalDealsCompleted Int               @default(0)
  totalDealsCancelled Int               @default(0)
  avgRating         Float               @default(0)
  ratingCount       Int                 @default(0)
  badge             BrokerBadge?        // TRUSTED | GOLD | PLATINUM
  isAcceptingLeads  Boolean             @default(true)
  createdAt         DateTime            @default(now())
}

enum BrokerBadge {
  TRUSTED   // 10+ deals
  GOLD      // 25+ deals
  PLATINUM  // 50+ deals
}

model DealerProfile {
  id                String              @id @default(cuid())
  userId            String              @unique
  user              User                @relation(fields: [userId], references: [id])
  businessName      String
  logo              String?
  coverPhoto        String?
  about             String?             @db.Text
  address           String
  latitude          Float
  longitude         Float
  phone             String
  whatsapp          String?
  website           String?
  workingHours      Json?               // structured weekly schedule
  package           DealerPackage       // SMALL | MEDIUM | LARGE
  packageExpiresAt  DateTime
  verificationState VerificationState   @default(PENDING)
  documentUrl       String              // commercial registration
  createdAt         DateTime            @default(now())
}

enum DealerPackage { SMALL  MEDIUM  LARGE }
enum OfficerStatus { ACTIVE  RETIRED }
enum VerificationState { PENDING  VERIFIED  REJECTED }

// ============ EXEMPTION MARKETPLACE ============

model ExemptionListing {
  id              String        @id @default(cuid())
  officerId       String
  officer         User          @relation(fields: [officerId], references: [id])
  vehiclePreference String?     // free text: what they want to buy
  bodyType        String?       // SEDAN | SUV | etc
  fuelType        String?       // GASOLINE | HYBRID | EV
  yearMin         Int?
  yearMax         Int?
  estimatedExemptionValue Int?  // savings in JOD
  estimatedSplit  Int?          // officer's expected share
  governorate     String
  notes           String?       @db.Text
  status          ListingStatus @default(ACTIVE)
  viewCount       Int           @default(0)
  createdAt       DateTime      @default(now())
  expiresAt       DateTime?
}

model Lead {
  id              String      @id @default(cuid())
  buyerId         String
  buyer           User        @relation("leadBuyer", fields: [buyerId], references: [id])
  vehicleType     String
  makeModel       String?
  yearMin         Int?
  yearMax         Int?
  budgetMin       Int?
  budgetMax       Int?
  governorate     String
  notes           String?     @db.Text
  status          LeadStatus  @default(OPEN)
  matchedBrokerId String?
  createdAt       DateTime    @default(now())
  expiresAt       DateTime
  closedAt        DateTime?

  reservations    LeadReservation[]
  deal            Deal?
}

enum LeadStatus { OPEN  RESERVED  IN_NEGOTIATION  CONVERTED  CLOSED  EXPIRED }

model LeadReservation {
  id              String              @id @default(cuid())
  leadId          String
  lead            Lead                @relation(fields: [leadId], references: [id])
  brokerId        String
  broker          User                @relation(fields: [brokerId], references: [id])
  reservedAt      DateTime            @default(now())
  expiresAt       DateTime            // 24h after reservation
  reservationFee  Int                 // 2 JOD
  status          ReservationStatus   @default(ACTIVE)
  refunded        Boolean             @default(false)
}

enum ReservationStatus { ACTIVE  CONVERTED  EXPIRED  RELEASED }

model Deal {
  id              String        @id @default(cuid())
  leadId          String?       @unique
  lead            Lead?         @relation(fields: [leadId], references: [id])
  buyerId         String
  buyer           User          @relation("dealBuyer", fields: [buyerId], references: [id])
  brokerId        String?       // null for direct officer deals
  broker          User?         @relation("dealBroker", fields: [brokerId], references: [id])
  officerId       String?       // null until officer matched
  officer         User?         @relation("dealOfficer", fields: [officerId], references: [id])
  vehicleDetails  Json          // structured: make, model, year, etc
  agreedPrice     Int?
  commissionAmount Int?
  status          DealStatus    @default(NEGOTIATION)
  buyerAgreedAt   DateTime?
  brokerAgreedAt  DateTime?
  identityRevealedAt DateTime?
  buyerPaidRevealFee Boolean   @default(false)
  brokerPaidCommission Boolean @default(false)
  contractSignedAt DateTime?
  completedAt     DateTime?
  cancelledAt     DateTime?
  cancellationReason String?
  createdAt       DateTime      @default(now())
  updatedAt       DateTime      @updatedAt

  timeline        DealTimelineEvent[]
  documents       DealDocument[]
  conversation    Conversation?
}

enum DealStatus {
  NEGOTIATION
  AGREED
  REVEALED
  CONTRACT_SIGNED
  WAITING_PERIOD
  TRANSFER_IN_PROGRESS
  COMPLETED
  CANCELLED
  DISPUTED
}

model DealTimelineEvent {
  id        String   @id @default(cuid())
  dealId    String
  deal      Deal     @relation(fields: [dealId], references: [id])
  eventType String   // STATUS_CHANGE | NOTE | DOCUMENT_ADDED | PAYMENT | etc
  payload   Json
  actorId   String
  createdAt DateTime @default(now())
}

model DealDocument {
  id          String   @id @default(cuid())
  dealId      String
  deal        Deal     @relation(fields: [dealId], references: [id])
  documentType String  // CONTRACT | OFFICIAL_PAPER | RECEIPT | PHOTO
  url         String
  uploadedBy  String
  createdAt   DateTime @default(now())
}

// ============ CAR MARKETPLACE ============

model CarListing {
  id              String          @id @default(cuid())
  sellerId        String
  seller          User            @relation(fields: [sellerId], references: [id])
  
  make            String
  model           String
  year            Int
  trim            String?
  bodyType        BodyType
  fuelType        FuelType
  transmission    Transmission
  mileage         Int
  color           String
  condition       VehicleCondition
  
  price           Int
  isNegotiable    Boolean         @default(true)
  
  title           String
  description     String          @db.Text
  photos          String[]
  videoUrl        String?
  
  governorate     String
  city            String
  latitude        Float
  longitude       Float
  locationAccuracy LocationAccuracy @default(APPROXIMATE)
  
  tier            ListingTier     @default(FREE)
  tierExpiresAt   DateTime?
  isAutoRepost    Boolean         @default(false)
  
  status          ListingStatus   @default(ACTIVE)
  viewCount       Int             @default(0)
  contactCount    Int             @default(0)
  saveCount       Int             @default(0)
  
  createdAt       DateTime        @default(now())
  updatedAt       DateTime        @updatedAt
  expiresAt       DateTime
  
  @@index([latitude, longitude])
  @@index([tier, status])
  @@index([make, model, year])
  @@index([governorate, status])
}

enum BodyType { SEDAN  SUV  HATCHBACK  PICKUP  COUPE  VAN  OTHER }
enum FuelType { GASOLINE  DIESEL  HYBRID  ELECTRIC  LPG }
enum Transmission { AUTOMATIC  MANUAL  CVT }
enum VehicleCondition { NEW  EXCELLENT  GOOD  FAIR  NEEDS_WORK }
enum LocationAccuracy { EXACT  APPROXIMATE  CITY_ONLY }
enum ListingTier { FREE  FEATURED  VIP  GOLD }
enum ListingStatus { ACTIVE  PAUSED  SOLD  EXPIRED  REMOVED }

// ============ WALLET & PAYMENTS ============

model Wallet {
  id          String   @id @default(cuid())
  userId      String   @unique
  user        User     @relation(fields: [userId], references: [id])
  balance     Int      @default(0)  // in fils (1 JOD = 1000 fils) for precision
  currency    String   @default("JOD")
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt
  
  transactions WalletTransaction[]
}

model WalletTransaction {
  id              String                @id @default(cuid())
  walletId        String
  wallet          Wallet                @relation(fields: [walletId], references: [id])
  type            TransactionType
  amount          Int                   // negative for debits, positive for credits
  balanceAfter    Int
  description     String
  relatedEntityType String?             // LEAD | DEAL | LISTING | TOPUP | etc
  relatedEntityId String?
  paymentGatewayRef String?             // for top-ups
  status          TransactionStatus     @default(COMPLETED)
  createdAt       DateTime              @default(now())
}

enum TransactionType {
  TOPUP
  BONUS
  LEAD_RESERVATION
  ACTIVITY_FEE
  COMMISSION
  REVEAL_FEE
  TRACKING_FEE
  LISTING_UPGRADE
  REFUND
  WITHDRAWAL
  PENALTY
}

enum TransactionStatus { PENDING  COMPLETED  FAILED  REVERSED }

// ============ CHAT & COMMUNICATION ============

model Conversation {
  id              String        @id @default(cuid())
  user1Id         String
  user1           User          @relation("user1", fields: [user1Id], references: [id])
  user2Id         String
  user2           User          @relation("user2", fields: [user2Id], references: [id])
  contextType     ContextType   // EXEMPTION_DEAL | CAR_LISTING | DIRECT
  contextId       String?       // dealId, carListingId, etc
  dealId          String?       @unique
  deal            Deal?         @relation(fields: [dealId], references: [id])
  isBlind         Boolean       @default(false) // true for exemption pre-reveal
  lastMessageAt   DateTime      @default(now())
  createdAt       DateTime      @default(now())
  
  messages        Message[]
}

enum ContextType { EXEMPTION_DEAL  CAR_LISTING  DIRECT }

model Message {
  id              String        @id @default(cuid())
  conversationId  String
  conversation    Conversation  @relation(fields: [conversationId], references: [id])
  senderId        String
  sender          User          @relation("sender", fields: [senderId], references: [id])
  content         String        @db.Text
  contentType     MessageType   @default(TEXT)
  attachmentUrl   String?
  isRead          Boolean       @default(false)
  isFlagged       Boolean       @default(false)
  flagReason      String?       // for moderation
  isBlocked       Boolean       @default(false) // hidden due to violation
  createdAt       DateTime      @default(now())
}

enum MessageType { TEXT  IMAGE  VOICE  SYSTEM }

// ============ SUPPORTING TABLES ============

model SavedListing {
  id              String   @id @default(cuid())
  userId          String
  user            User     @relation(fields: [userId], references: [id])
  listingType     String   // EXEMPTION | CAR
  listingId       String
  createdAt       DateTime @default(now())
  
  @@unique([userId, listingType, listingId])
}

model Rating {
  id          String   @id @default(cuid())
  raterId     String
  rater       User     @relation("rater", fields: [raterId], references: [id])
  ratedUserId String
  ratedUser   User     @relation("rated", fields: [ratedUserId], references: [id])
  dealId      String?
  stars       Int      // 1-5
  comment     String?  @db.Text
  isPublic    Boolean  @default(true)
  createdAt   DateTime @default(now())
  
  @@unique([raterId, ratedUserId, dealId])
}

model Report {
  id          String       @id @default(cuid())
  reporterId  String
  reporter    User         @relation("reporter", fields: [reporterId], references: [id])
  reportedId  String?
  reported    User?        @relation("reported", fields: [reportedId], references: [id])
  targetType  ReportTarget
  targetId    String
  reason      String
  description String?      @db.Text
  status      ReportStatus @default(OPEN)
  resolution  String?
  resolvedBy  String?
  resolvedAt  DateTime?
  createdAt   DateTime     @default(now())
}

enum ReportTarget { USER  EXEMPTION_LISTING  CAR_LISTING  MESSAGE  REVIEW }
enum ReportStatus { OPEN  INVESTIGATING  RESOLVED  DISMISSED }

model Notification {
  id          String           @id @default(cuid())
  userId      String
  user        User             @relation(fields: [userId], references: [id])
  type        NotificationType
  title       String
  body        String
  data        Json?
  isRead      Boolean          @default(false)
  createdAt   DateTime         @default(now())
}

enum NotificationType {
  NEW_MESSAGE
  NEW_LEAD_MATCH
  LEAD_RESERVED
  DEAL_AGREED
  IDENTITY_REVEALED
  VERIFICATION_APPROVED
  VERIFICATION_REJECTED
  LISTING_EXPIRED
  WALLET_LOW
  PAYMENT_SUCCESS
  SYSTEM
}

// Vehicle catalog (preloaded reference data)
model VehicleCatalog {
  id          String   @id @default(cuid())
  make        String
  model       String
  yearStart   Int
  yearEnd     Int?
  bodyType    BodyType
  isEligibleForExemption Boolean @default(false)
  estimatedExemptionValue Int?
  
  @@unique([make, model, yearStart])
}
```

---

## 7. API Surface (REST)

Base: `/api/v1`

### Auth
- `POST /auth/verify-firebase-token` — exchange Firebase ID token for JWT
- `POST /auth/refresh`
- `POST /auth/logout`
- `DELETE /auth/account`

### Users & Profiles
- `GET /users/me`
- `PATCH /users/me`
- `POST /users/me/roles/:role` — add role (officer/broker/dealer)
- `POST /users/me/officer-profile`
- `POST /users/me/broker-profile`
- `POST /users/me/dealer-profile`
- `GET /users/:id/public-profile` — limited info

### Wallet
- `GET /wallet/balance`
- `GET /wallet/transactions`
- `POST /wallet/topup` — initiates HyperPay flow
- `POST /wallet/topup/callback` — payment gateway webhook
- `POST /wallet/withdraw` — request payout

### Exemption Marketplace
**Officer side:**
- `POST /exemptions/listings`
- `GET /exemptions/listings/mine`
- `PATCH /exemptions/listings/:id`
- `DELETE /exemptions/listings/:id`

**Buyer side:**
- `POST /leads` — create a need
- `GET /leads/mine`
- `PATCH /leads/:id`

**Broker side:**
- `GET /broker/leads/feed` — sorted by match score
- `POST /broker/leads/:id/reserve` — costs 2 JOD
- `GET /broker/deals` — pipeline view (Kanban data)
- `POST /broker/deals` — create deal from reserved lead
- `PATCH /broker/deals/:id/status`
- `POST /broker/deals/:id/notes`

**Deal flow:**
- `POST /deals/:id/agree` — buyer or broker clicks "Agree"
- `POST /deals/:id/pay-reveal` — buyer pays reveal fee
- `POST /deals/:id/pay-commission` — broker pays commission
- `GET /deals/:id` — full deal details (filtered by role)
- `POST /deals/:id/complete` — mark deal as done
- `POST /deals/:id/cancel`
- `POST /deals/:id/dispute`

### Car Marketplace
- `POST /cars/listings` — create with tier selection
- `GET /cars/listings` — search with filters
- `GET /cars/listings/map` — geo-bounded query for map view
- `GET /cars/listings/:id`
- `PATCH /cars/listings/:id`
- `DELETE /cars/listings/:id`
- `POST /cars/listings/:id/upgrade` — upgrade to higher tier
- `POST /cars/listings/:id/save`
- `POST /cars/listings/:id/report-contact` — analytics

### Dealer
- `GET /dealers` — public dealer directory
- `GET /dealers/:id` — dealer page with listings
- `POST /dealer/listings/bulk` — CSV/Excel upload (Large package)
- `GET /dealer/analytics`

### Chat
- `GET /conversations`
- `GET /conversations/:id/messages?cursor=&limit=`
- `POST /conversations/:id/messages`
- `POST /conversations/:id/voice-call` — initiate LiveKit session
- WebSocket: `/ws/chat`

### Map Search
- `GET /search/map?swLat=&swLng=&neLat=&neLng=&filters=` — bounding box
- `GET /search/heatmap?type=price&zoom=`
- `POST /search/save-area` — save custom drawn area
- `GET /search/saved`

### Saved Searches & Notifications
- `GET /saved-searches`
- `POST /saved-searches`
- `DELETE /saved-searches/:id`
- `GET /notifications`
- `PATCH /notifications/:id/read`
- `POST /notifications/mark-all-read`

### Ratings & Reports
- `POST /ratings`
- `GET /users/:id/ratings`
- `POST /reports`

### Admin
- `GET /admin/verifications?type=officer|broker|dealer`
- `POST /admin/verifications/:id/approve`
- `POST /admin/verifications/:id/reject`
- `GET /admin/reports`
- `POST /admin/users/:id/block`
- `GET /admin/disputes`
- `POST /admin/disputes/:id/resolve`
- `GET /admin/analytics` — platform-wide stats
- `GET /admin/flagged-messages`

---

## 8. Security & Privacy Requirements

Non-negotiable:

1. **Document encryption**: All identity documents (military IDs, commercial 
   registrations) stored in Firebase Storage with signed, time-limited URLs. 
   Only the uploader and admin can read.
2. **Database encryption at rest** + application-layer encryption for sensitive fields.
3. **No PII in logs**: Never log full names, phone numbers, document URLs.
4. **HTTPS only** with HSTS; TLS 1.2+ enforced.
5. **Rate limiting** per IP and per user on every endpoint.
6. **CSRF protection** on admin panel.
7. **SQL injection protection** via Prisma's parameterized queries.
8. **Anti-fraud**: Firebase App Check on Android; SMS region restricted to +962.
9. **Account deletion**: Wipes all PII within 30 days (chat content, photos, docs).
10. **Backup encryption** with separate key, stored off-prod.
11. **Audit log** for admin actions (verification approvals, blocks, refunds).
12. **PCI compliance**: Never store payment card data; all card handling via 
    HyperPay/Madfoatcom.

---

## 9. UI/UX Guidelines

### Visual Identity
- **Palette:** Deep navy (#0F2A47) primary, Gold (#C9A961) secondary, 
  White background, charcoal text. Serious, trustworthy.
- **Font:** Cairo (Arabic + Latin), body min 16sp, headings up to 32sp
- **Icons:** Material Symbols, outlined style
- **Imagery:** Real photos preferred over illustrations; subtle military/automotive 
  motifs without being kitsch

### Layout Principles
- **RTL first** — every screen designed in Arabic, English is mirror
- **Bottom nav** with 5 tabs: Cars Map | Search | Post | Messages | Profile
- **Floating filter pill** at top of map and list views
- **Card-based lists** with consistent shadow, padding, corner radius (12dp)

### Empty States
- Every list has a designed empty state with clear next action
- Illustrations should be culturally relevant (Jordanian context)

### Loading & Errors
- Skeleton loaders, not spinners
- All error messages in Arabic, plain language
- Never expose technical errors to user
- Retry actions where appropriate

### Accessibility
- TalkBack support
- Dynamic font scaling
- WCAG AA contrast minimum
- All interactive elements ≥ 48dp tap target

### RTL Specifics
- Numbers always LTR even in Arabic context (e.g., "25,000 د.أ")
- Phone numbers LTR
- Latin words inline in Arabic text use LTR isolation

---

## 10. Sprint Plan (Revised)

### Phase 1: Foundation (Sprints 1–4)
- ✅ **Sprint 1:** Project skeleton (DONE)
- ✅ **Sprint 2:** Phone OTP auth (DONE)
- **Sprint 3:** Officer verification + admin panel skeleton
- **Sprint 4:** Wallet system + HyperPay integration

### Phase 2: Car Marketplace MVP (Sprints 5–8)
- **Sprint 5:** Car listing creation + photo upload
- **Sprint 6:** Map-based discovery + filters
- **Sprint 7:** Chat (non-blind) + push notifications
- **Sprint 8:** Listing tiers + payment flow

### Phase 3: Exemption Marketplace (Sprints 9–12)
- **Sprint 9:** Exemption listings + leads system
- **Sprint 10:** Broker profiles + lead reservation
- **Sprint 11:** Deal flow + blind chat + reveal mechanics
- **Sprint 12:** Anti-circumvention (AI moderation) + tracking

### Phase 4: Polish & Advanced (Sprints 13–16)
- **Sprint 13:** Dealer packages + bulk upload
- **Sprint 14:** Ratings, reports, dispute resolution
- **Sprint 15:** LiveKit voice calls (blind calling)
- **Sprint 16:** Pre-launch polish, beta testing, Play Store

---

## 11. Legal Disclaimers (Must Appear In App)

**First-launch welcome:**
> "رائد منصة تعارف ووساطة بين الأطراف فقط. نحن لا نشتري أو نبيع المركبات أو الإعفاءات، 
> ولسنا طرفًا في أي اتفاق. جميع المعاملات مسؤولية المستخدمين."

**Before officer creates an exemption listing:**
> "بنشرك هذا الإعلان، تقرّ بأنك تمتلك حق الإعفاء وأنك لم تستخدمه سابقًا. 
> أي تصريح كاذب مسؤوليتك القانونية الشخصية."

**Before broker reserves a lead:**
> "حجزك لهذا الطلب يلزمك بمعاملته خلال 24 ساعة. رسم الحجز غير مسترد إلا في 
> حالات محددة في شروط الاستخدام."

**In every blind chat:**
> "ينصح باستشارة محامٍ قبل توقيع أي عقد. تبادل أرقام التواصل خارج التطبيق مخالف 
> للسياسة وقد يؤدي لتعليق الحساب."

**On wallet top-up:**
> "رصيد المحفظة قابل للسحب في أي وقت بعد خصم رسوم المعاملات. عمليات الدفع 
> معالجة عبر بوابة آمنة معتمدة."

---

## 12. Open Questions / Pending Decisions

1. **Legal counsel review** — has a Jordanian lawyer reviewed:
   - The exemption brokerage concept
   - Wallet/escrow operations (do we need Central Bank licensing?)
   - Terms of Service draft
   - Privacy Policy draft
2. **Tax treatment** — what taxes apply to platform commissions?
3. **Verification capacity** — who handles document review during scale?
4. **Partner outreach** — initial partnerships:
   - Lawyers for sample contracts
   - Mechanics for inspection service
   - Banks for financing
   - Insurance companies
5. **Marketing budget** — Facebook ads, broker outreach, ambassadors program?
6. **Beta tester recruitment** — target 10 officers + 10 brokers + 20 buyers for closed beta

---

## 13. Success Metrics (Phase 1 Goals)

**Month 3 post-launch:**
- 500 registered users
- 50 verified officers
- 10 verified brokers
- 200 active car listings
- 20 completed exemption deals

**Month 6:**
- 2,000 users
- 150 officers, 30 brokers
- 1,000 car listings, 100 dealer accounts
- 100 completed exemption deals/month
- Break-even on operational costs

**Month 12:**
- 10,000 users
- Multi-revenue stream stability
- Expansion considerations (Palestine, other GCC?)

---

## 14. What's Currently Built (As of May 2026)

**Sprint 1 ✅** — Project skeleton (backend + Android shells)
**Sprint 2 ✅** — Phone OTP authentication end-to-end

**Next:** Sprint 3 — Officer verification flow

---

**End of Master Specification v2.0**
