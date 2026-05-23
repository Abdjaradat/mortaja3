-- Add MEDICAL_EXEMPT to UserType enum
ALTER TYPE "UserType" ADD VALUE IF NOT EXISTS 'MEDICAL_EXEMPT';

-- Create ListingCategory enum
DO $$ BEGIN
  CREATE TYPE "ListingCategory" AS ENUM ('MORTAJA3', 'REGULAR', 'EXEMPTION_RIGHT');
EXCEPTION
  WHEN duplicate_object THEN null;
END $$;

-- Add new columns to Listing
ALTER TABLE "Listing"
    ADD COLUMN IF NOT EXISTS "listingCategory"   "ListingCategory" NOT NULL DEFAULT 'MORTAJA3',
    ADD COLUMN IF NOT EXISTS "sellerType"         TEXT,
    ADD COLUMN IF NOT EXISTS "restrictionEndsAt"  TIMESTAMP(3),
    ADD COLUMN IF NOT EXISTS "originalPrice"      INTEGER;

-- Backfill: all existing OWNED/SEEKING listings are exemption-related
UPDATE "Listing" SET "listingCategory" = 'EXEMPTION_RIGHT'
WHERE "listingType" IN ('OWNED', 'SEEKING');

-- Create MedicalExemptProfile table
CREATE TABLE IF NOT EXISTS "MedicalExemptProfile" (
    "id"                TEXT NOT NULL,
    "userId"            TEXT NOT NULL,
    "documentUrl"       TEXT NOT NULL,
    "verificationState" "VerificationState" NOT NULL DEFAULT 'PENDING',
    "verifiedAt"        TIMESTAMP(3),
    "exemptionUsed"     BOOLEAN NOT NULL DEFAULT false,
    "createdAt"         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "MedicalExemptProfile_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS "MedicalExemptProfile_userId_key"
    ON "MedicalExemptProfile"("userId");

ALTER TABLE "MedicalExemptProfile"
    DROP CONSTRAINT IF EXISTS "MedicalExemptProfile_userId_fkey";

ALTER TABLE "MedicalExemptProfile"
    ADD CONSTRAINT "MedicalExemptProfile_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "User"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;
