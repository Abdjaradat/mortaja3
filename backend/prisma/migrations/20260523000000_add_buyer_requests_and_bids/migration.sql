-- New enum
CREATE TYPE "RequestStatus" AS ENUM ('OPEN', 'CLOSED', 'EXPIRED');

-- BuyerRequest table
CREATE TABLE "BuyerRequest" (
    "id"          TEXT NOT NULL,
    "buyerId"     TEXT NOT NULL,
    "vehicleType" TEXT NOT NULL,
    "budgetMin"   INTEGER NOT NULL,
    "budgetMax"   INTEGER NOT NULL,
    "governorate" TEXT NOT NULL,
    "notes"       TEXT,
    "status"      "RequestStatus" NOT NULL DEFAULT 'OPEN',
    "expiresAt"   TIMESTAMP(3) NOT NULL,
    "createdAt"   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "BuyerRequest_pkey" PRIMARY KEY ("id")
);

-- Bid table
CREATE TABLE "Bid" (
    "id"        TEXT NOT NULL,
    "requestId" TEXT NOT NULL,
    "brokerId"  TEXT NOT NULL,
    "tokens"    INTEGER NOT NULL,
    "placedAt"  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "isWinner"  BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT "Bid_pkey" PRIMARY KEY ("id")
);

-- Unique: one bid per broker per request
CREATE UNIQUE INDEX "Bid_requestId_brokerId_key" ON "Bid"("requestId", "brokerId");

-- Foreign keys
ALTER TABLE "BuyerRequest" ADD CONSTRAINT "BuyerRequest_buyerId_fkey"
    FOREIGN KEY ("buyerId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "Bid" ADD CONSTRAINT "Bid_requestId_fkey"
    FOREIGN KEY ("requestId") REFERENCES "BuyerRequest"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "Bid" ADD CONSTRAINT "Bid_brokerId_fkey"
    FOREIGN KEY ("brokerId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
