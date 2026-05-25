-- AlterEnum
ALTER TYPE "TxReason" ADD VALUE 'LISTING_SHARE';

-- CreateTable
CREATE TABLE "ShareLog" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "platform" TEXT NOT NULL,
    "tokensEarned" INTEGER NOT NULL DEFAULT 10,
    "sharedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ShareLog_pkey" PRIMARY KEY ("id")
);

-- AddForeignKey
ALTER TABLE "ShareLog" ADD CONSTRAINT "ShareLog_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
