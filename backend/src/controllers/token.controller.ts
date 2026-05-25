import { Response } from "express";
import prisma from "../utils/prisma.js";
import { applyTokens, AD_WATCH_DAILY_LIMIT } from "../utils/tokens.js";
import { TxReason } from "@prisma/client";
import type { AuthenticatedRequest } from "../middleware/auth.js";
import { sendPushNotification } from "../utils/firebase.js";

export async function getTokenBalance(req: AuthenticatedRequest, res: Response): Promise<void> {
  const userId = req.user!.userId;

  const user = await prisma.user.findUniqueOrThrow({
    where: { id: userId },
    select: { tokenBalance: true, totalTokensEarned: true, totalTokensSpent: true, referralCode: true },
  });

  const transactions = await prisma.tokenTransaction.findMany({
    where:   { userId },
    orderBy: { createdAt: "desc" },
    take:    30,
    select:  { id: true, type: true, amount: true, reason: true, balanceAfter: true, createdAt: true },
  });

  res.json({ ...user, transactions });
}

export async function watchAd(req: AuthenticatedRequest, res: Response): Promise<void> {
  const userId = req.user!.userId;

  // Enforce daily limit
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);

  const todayCount = await prisma.adWatchLog.count({
    where: { userId, watchedAt: { gte: startOfDay } },
  });

  if (todayCount >= AD_WATCH_DAILY_LIMIT) {
    res.status(429).json({
      error: "DAILY_LIMIT_REACHED",
      message: `لقد وصلت للحد اليومي (${AD_WATCH_DAILY_LIMIT} مشاهدة)`,
    });
    return;
  }

  const [{ balanceAfter }] = await Promise.all([
    applyTokens(userId, TxReason.AD_WATCH),
    prisma.adWatchLog.create({ data: { userId, tokensEarned: 10 } }),
  ]);

  prisma.user.findUnique({ where: { id: userId }, select: { fcmToken: true } })
    .then((user) => {
      if (user?.fcmToken) {
        sendPushNotification(
          user.fcmToken,
          "🪙 +10 توكن!",
          `تمت إضافة 10 توكن لرصيدك. رصيدك الآن: ${balanceAfter} توكن`,
          { type: "TOKENS_EARNED", amount: "10" },
        ).catch(() => {});
      }
    })
    .catch(() => {});

  res.json({ tokensEarned: 10, balanceAfter, todayCount: todayCount + 1 });
}

export async function earnShare(req: AuthenticatedRequest, res: Response): Promise<void> {
  const userId = req.user!.userId;
  const { platform } = req.body as { platform?: string };

  const SHARE_DAILY_LIMIT = 3;
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);

  const todayCount = await prisma.shareLog.count({
    where: { userId, sharedAt: { gte: startOfDay } },
  });

  if (todayCount >= SHARE_DAILY_LIMIT) {
    res.status(429).json({
      error: "DAILY_LIMIT_REACHED",
      message: `لقد وصلت للحد اليومي (${SHARE_DAILY_LIMIT} مشاركات)`,
      remainingToday: 0,
    });
    return;
  }

  const [{ balanceAfter }] = await Promise.all([
    applyTokens(userId, TxReason.LISTING_SHARE),
    prisma.shareLog.create({ data: { userId, platform: platform ?? "unknown", tokensEarned: 10 } }),
  ]);

  res.json({ tokensEarned: 10, newBalance: balanceAfter, remainingToday: SHARE_DAILY_LIMIT - todayCount - 1 });
}

export async function spendTokens(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { reason, relatedEntityId } = req.body as {
    reason?: string;
    relatedEntityId?: string;
  };

  const validReasons = Object.values(TxReason).filter(
    (r) => !["WELCOME", "AD_WATCH", "REFERRAL", "PURCHASE"].includes(r),
  );

  if (!reason || !validReasons.includes(reason as TxReason)) {
    res.status(400).json({ error: "reason غير صالح", valid: validReasons });
    return;
  }

  try {
    const { balanceAfter } = await applyTokens(
      req.user!.userId,
      reason as TxReason,
      relatedEntityId,
    );
    res.json({ balanceAfter });
  } catch (err: unknown) {
    if (err instanceof Error && err.message.startsWith("INSUFFICIENT_TOKENS")) {
      const current = err.message.split(":")[1];
      res.status(402).json({
        error: "INSUFFICIENT_TOKENS",
        currentBalance: Number(current),
        message: "رصيد التوكنز غير كافٍ",
      });
    } else {
      res.status(500).json({ error: "خطأ غير متوقع" });
    }
  }
}
