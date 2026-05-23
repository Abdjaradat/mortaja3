-- Extend TxReason enum
ALTER TYPE "TxReason" ADD VALUE IF NOT EXISTS 'REGISTER_CLEARANCE_AGENT';
ALTER TYPE "TxReason" ADD VALUE IF NOT EXISTS 'CLEARANCE_OFFER';
ALTER TYPE "TxReason" ADD VALUE IF NOT EXISTS 'CLEARANCE_SELECT';
ALTER TYPE "TxReason" ADD VALUE IF NOT EXISTS 'CLEARANCE_RATE_BONUS';

-- New enums
CREATE TYPE "ClearanceLocation" AS ENUM ('BOHRET_AMMAN', 'ZARQA');
CREATE TYPE "ClearanceStatus"   AS ENUM ('OPEN', 'CLOSED', 'EXPIRED');

-- ClearanceAgent
CREATE TABLE "ClearanceAgent" (
    "id"              TEXT          NOT NULL,
    "userId"          TEXT          NOT NULL,
    "displayName"     TEXT          NOT NULL,
    "photoUrl"        TEXT,
    "location"        "ClearanceLocation" NOT NULL,
    "specializations" TEXT[]        NOT NULL DEFAULT '{}',
    "yearsExperience" INTEGER       NOT NULL DEFAULT 0,
    "bio"             TEXT,
    "isVerified"      BOOLEAN       NOT NULL DEFAULT false,
    "totalDeals"      INTEGER       NOT NULL DEFAULT 0,
    "avgRating"       DOUBLE PRECISION NOT NULL DEFAULT 0,
    "ratingCount"     INTEGER       NOT NULL DEFAULT 0,
    "createdAt"       TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "ClearanceAgent_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "ClearanceAgent_userId_key" ON "ClearanceAgent"("userId");

ALTER TABLE "ClearanceAgent"
    ADD CONSTRAINT "ClearanceAgent_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- ClearanceRequest
CREATE TABLE "ClearanceRequest" (
    "id"          TEXT          NOT NULL,
    "customerId"  TEXT          NOT NULL,
    "serviceType" TEXT          NOT NULL,
    "location"    "ClearanceLocation" NOT NULL,
    "description" TEXT          NOT NULL,
    "budgetMax"   INTEGER,
    "status"      "ClearanceStatus" NOT NULL DEFAULT 'OPEN',
    "isRated"     BOOLEAN       NOT NULL DEFAULT false,
    "expiresAt"   TIMESTAMP(3)  NOT NULL,
    "createdAt"   TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "ClearanceRequest_pkey" PRIMARY KEY ("id")
);

ALTER TABLE "ClearanceRequest"
    ADD CONSTRAINT "ClearanceRequest_customerId_fkey"
    FOREIGN KEY ("customerId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- ClearanceOffer
CREATE TABLE "ClearanceOffer" (
    "id"         TEXT         NOT NULL,
    "requestId"  TEXT         NOT NULL,
    "agentId"    TEXT         NOT NULL,
    "price"      INTEGER      NOT NULL,
    "notes"      TEXT,
    "isSelected" BOOLEAN      NOT NULL DEFAULT false,
    "placedAt"   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "ClearanceOffer_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "ClearanceOffer_requestId_agentId_key"
    ON "ClearanceOffer"("requestId", "agentId");

ALTER TABLE "ClearanceOffer"
    ADD CONSTRAINT "ClearanceOffer_requestId_fkey"
    FOREIGN KEY ("requestId") REFERENCES "ClearanceRequest"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "ClearanceOffer"
    ADD CONSTRAINT "ClearanceOffer_agentId_fkey"
    FOREIGN KEY ("agentId") REFERENCES "ClearanceAgent"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
