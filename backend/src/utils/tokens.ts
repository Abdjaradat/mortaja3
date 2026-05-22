import prisma from "./prisma.js";
import { TxReason, TxType } from "@prisma/client";

export const TOKEN_COSTS: Record<TxReason, number> = {
  WELCOME:            500,
  AD_WATCH:           10,
  REFERRAL:           100,
  POST_LISTING:       -50,
  POST_EXEMPTION:     -30,
  REVEAL_CONTACT:     -20,
  START_CONVERSATION: -10,
  BOOST_LISTING:      -100,
  RENEW_LISTING:      -25,
  PURCHASE:           0,   // variable, passed explicitly
};

export const AD_WATCH_DAILY_LIMIT = 20;

/** Generate a short unique referral code like "RC4F2A1B" */
export function generateReferralCode(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "RC";
  for (let i = 0; i < 6; i++) {
    code += chars[Math.floor(Math.random() * chars.length)];
  }
  return code;
}

/**
 * Atomically credit or debit tokens for a user.
 * Returns the updated balance, or throws if insufficient funds.
 */
export async function applyTokens(
  userId:          string,
  reason:          TxReason,
  relatedEntityId?: string,
  overrideAmount?: number,
): Promise<{ balanceAfter: number }> {
  const amount = overrideAmount ?? TOKEN_COSTS[reason];

  // Use a transaction to read-then-write atomically
  const result = await prisma.$transaction(async (tx) => {
    const user = await tx.user.findUniqueOrThrow({
      where: { id: userId },
      select: { tokenBalance: true },
    });

    const newBalance = user.tokenBalance + amount;
    if (newBalance < 0) {
      throw new Error(`INSUFFICIENT_TOKENS:${user.tokenBalance}`);
    }

    const [updated] = await Promise.all([
      tx.user.update({
        where: { id: userId },
        data: {
          tokenBalance:     newBalance,
          totalTokensEarned: amount > 0 ? { increment: amount } : undefined,
          totalTokensSpent:  amount < 0 ? { increment: -amount } : undefined,
        },
        select: { tokenBalance: true },
      }),
      tx.tokenTransaction.create({
        data: {
          userId,
          type:           amount >= 0 ? TxType.EARN : TxType.SPEND,
          amount,
          reason,
          balanceAfter:   newBalance,
          relatedEntityId: relatedEntityId ?? null,
        },
      }),
    ]);

    return updated;
  });

  return { balanceAfter: result.tokenBalance };
}
