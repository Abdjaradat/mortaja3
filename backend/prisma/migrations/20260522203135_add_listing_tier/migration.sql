-- ============================================================
-- Delta migration on top of 20260520220217_initial_schema
-- Adds: Firebase auth fields, token system, car listing fields,
--       ListingTier enum + columns
-- ============================================================

-- New enums
CREATE TYPE "TxType" AS ENUM ('EARN', 'SPEND');
CREATE TYPE "TxReason" AS ENUM ('WELCOME', 'AD_WATCH', 'REFERRAL', 'POST_LISTING', 'POST_EXEMPTION', 'REVEAL_CONTACT', 'START_CONVERSATION', 'BOOST_LISTING', 'RENEW_LISTING', 'PURCHASE');
CREATE TYPE "ListingTier" AS ENUM ('FREE', 'FEATURED', 'VIP', 'GOLD');

-- User: swap phoneNumber for Firebase auth fields
DROP INDEX "User_phoneNumber_key";
ALTER TABLE "User" DROP COLUMN "phoneNumber";
ALTER TABLE "User"
    ADD COLUMN "firebaseUid"       TEXT NOT NULL DEFAULT '',
    ADD COLUMN "email"             TEXT,
    ADD COLUMN "tokenBalance"      INTEGER NOT NULL DEFAULT 500,
    ADD COLUMN "totalTokensEarned" INTEGER NOT NULL DEFAULT 500,
    ADD COLUMN "totalTokensSpent"  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN "referralCode"      TEXT NOT NULL DEFAULT '',
    ADD COLUMN "referredBy"        TEXT;
ALTER TABLE "User" ALTER COLUMN "firebaseUid" DROP DEFAULT;
ALTER TABLE "User" ALTER COLUMN "referralCode" DROP DEFAULT;
CREATE UNIQUE INDEX "User_firebaseUid_key" ON "User"("firebaseUid");
CREATE UNIQUE INDEX "User_email_key"       ON "User"("email");
CREATE UNIQUE INDEX "User_referralCode_key" ON "User"("referralCode");

-- Listing: add car-specific fields + tier
ALTER TABLE "Listing"
    ADD COLUMN "mileageKm"     INTEGER,
    ADD COLUMN "fuelType"      TEXT,
    ADD COLUMN "transmission"  TEXT,
    ADD COLUMN "phoneNumber"   TEXT,
    ADD COLUMN "tier"          "ListingTier" NOT NULL DEFAULT 'FREE',
    ADD COLUMN "tierExpiresAt" TIMESTAMP(3);

-- New tables: token system
CREATE TABLE "TokenTransaction" (
    "id"              TEXT NOT NULL,
    "userId"          TEXT NOT NULL,
    "type"            "TxType" NOT NULL,
    "amount"          INTEGER NOT NULL,
    "reason"          "TxReason" NOT NULL,
    "balanceAfter"    INTEGER NOT NULL,
    "relatedEntityId" TEXT,
    "createdAt"       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "TokenTransaction_pkey" PRIMARY KEY ("id")
);
CREATE TABLE "AdWatchLog" (
    "id"           TEXT NOT NULL,
    "userId"       TEXT NOT NULL,
    "tokensEarned" INTEGER NOT NULL DEFAULT 10,
    "watchedAt"    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "AdWatchLog_pkey" PRIMARY KEY ("id")
);

-- Foreign keys for new tables
ALTER TABLE "TokenTransaction" ADD CONSTRAINT "TokenTransaction_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "AdWatchLog" ADD CONSTRAINT "AdWatchLog_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
