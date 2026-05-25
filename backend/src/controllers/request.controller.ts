import { Request, Response } from "express";
import { z } from "zod";
import prisma from "../utils/prisma.js";
import { applyTokens } from "../utils/tokens.js";
import type { AuthenticatedRequest } from "../middleware/auth.js";
import { RequestStatus } from "@prisma/client";
import { sendPushNotification } from "../utils/firebase.js";

const createRequestSchema = z.object({
  vehicleType: z.enum(["SEDAN", "SUV", "HYBRID", "EV", "OTHER"]),
  budgetMin:   z.number().int().positive(),
  budgetMax:   z.number().int().positive(),
  governorate: z.string().min(1),
  notes:       z.string().max(500).optional(),
});

const bidSchema = z.object({
  tokens: z.number().int().min(1),
});

const bidSelect = {
  id:       true,
  brokerId: true,
  tokens:   true,
  placedAt: true,
  isWinner: true,
  broker: { select: { id: true, fullName: true } },
} as const;

const requestSelect = {
  id:          true,
  buyerId:     true,
  vehicleType: true,
  budgetMin:   true,
  budgetMax:   true,
  governorate: true,
  notes:       true,
  status:      true,
  expiresAt:   true,
  createdAt:   true,
  buyer:       { select: { id: true, fullName: true } },
  bids:        { select: bidSelect, orderBy: { tokens: "desc" as const } },
} as const;

// POST /api/v1/requests
export async function createRequest(req: AuthenticatedRequest, res: Response): Promise<void> {
  const parsed = createRequestSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const { budgetMin, budgetMax } = parsed.data;
  if (budgetMax <= budgetMin) {
    res.status(400).json({ error: "budgetMax must be greater than budgetMin" });
    return;
  }

  const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000); // 24 h
  const request = await prisma.buyerRequest.create({
    data: { ...parsed.data, buyerId: req.user!.userId, expiresAt },
    select: requestSelect,
  });

  res.status(201).json(request);
}

// GET /api/v1/requests
export async function getRequests(req: AuthenticatedRequest, res: Response): Promise<void> {
  const userId   = req.user!.userId;
  const isBuyer  = req.query["mine"] === "true";
  const now      = new Date();

  const where = isBuyer
    ? { buyerId: userId }
    : { status: RequestStatus.OPEN, expiresAt: { gt: now } };

  const requests = await prisma.buyerRequest.findMany({
    where,
    select:  requestSelect,
    orderBy: { createdAt: "desc" },
  });

  res.json(requests);
}

// GET /api/v1/requests/:id
export async function getRequestById(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id } = req.params as { id: string };
  const request = await prisma.buyerRequest.findUnique({ where: { id }, select: requestSelect });

  if (!request) {
    res.status(404).json({ error: "Request not found" });
    return;
  }

  res.json(request);
}

// POST /api/v1/requests/:id/bid
export async function placeBid(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id }    = req.params as { id: string };
  const brokerId  = req.user!.userId;

  const parsed = bidSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }
  const { tokens } = parsed.data;

  const request = await prisma.buyerRequest.findUnique({
    where:  { id },
    select: { id: true, status: true, expiresAt: true, buyerId: true, bids: { where: { brokerId }, select: { id: true, tokens: true } } },
  });

  if (!request || request.status !== RequestStatus.OPEN || request.expiresAt < new Date()) {
    res.status(404).json({ error: "Request not found or already closed" });
    return;
  }

  if (request.buyerId === brokerId) {
    res.status(400).json({ error: "Cannot bid on your own request" });
    return;
  }

  const existingBid = request.bids[0] ?? null;

  try {
    const bid = await prisma.$transaction(async (tx) => {
      // Refund previous bid if exists
      if (existingBid) {
        const user = await tx.user.findUniqueOrThrow({ where: { id: brokerId }, select: { tokenBalance: true } });
        const refundedBalance = user.tokenBalance + existingBid.tokens;
        await tx.user.update({
          where: { id: brokerId },
          data: {
            tokenBalance:     refundedBalance,
            totalTokensSpent: { decrement: existingBid.tokens },
          },
        });
        await tx.tokenTransaction.create({
          data: {
            userId:          brokerId,
            type:            "EARN",
            amount:          existingBid.tokens,
            reason:          "BOOST_LISTING", // reusing closest enum; bid refund
            balanceAfter:    refundedBalance,
            relatedEntityId: id,
          },
        });
        await tx.bid.delete({ where: { id: existingBid.id } });
      }

      // Deduct new bid tokens
      const user = await tx.user.findUniqueOrThrow({ where: { id: brokerId }, select: { tokenBalance: true } });
      const newBalance = user.tokenBalance - tokens;
      if (newBalance < 0) {
        throw new Error(`INSUFFICIENT_TOKENS:${user.tokenBalance}`);
      }
      await tx.user.update({
        where: { id: brokerId },
        data: {
          tokenBalance:      newBalance,
          totalTokensSpent:  { increment: tokens },
        },
      });
      await tx.tokenTransaction.create({
        data: {
          userId:          brokerId,
          type:            "SPEND",
          amount:          -tokens,
          reason:          "BOOST_LISTING",
          balanceAfter:    newBalance,
          relatedEntityId: id,
        },
      });

      return tx.bid.create({
        data:   { requestId: id, brokerId, tokens },
        select: bidSelect,
      });
    });

    // Fire-and-forget: notify buyer of new bid
    prisma.user.findUnique({ where: { id: request.buyerId }, select: { fcmToken: true } })
      .then((buyer) => {
        if (buyer?.fcmToken) {
          sendPushNotification(
            buyer.fcmToken,
            "عرض جديد على طلبك 🎯",
            `تلقيت عرضاً جديداً بقيمة ${tokens} توكن. ادخل لمشاهدة التفاصيل.`,
            { type: "NEW_BID", requestId: id },
          ).catch(() => {});
        }
      })
      .catch(() => {});

    res.status(201).json(bid);
  } catch (err: unknown) {
    if (err instanceof Error && err.message.startsWith("INSUFFICIENT_TOKENS")) {
      const current = err.message.split(":")[1];
      res.status(402).json({ error: "insufficient_tokens", balance: Number(current) });
    } else {
      throw err;
    }
  }
}

// POST /api/v1/requests/:id/close
export async function closeRequest(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id }    = req.params as { id: string };
  const buyerId   = req.user!.userId;
  const winnerId: string | undefined = req.body?.winnerId;

  const request = await prisma.buyerRequest.findUnique({
    where:  { id },
    select: { id: true, status: true, buyerId: true, bids: { select: { id: true, brokerId: true, tokens: true } } },
  });

  if (!request || request.buyerId !== buyerId) {
    res.status(404).json({ error: "Request not found" });
    return;
  }

  if (request.status !== RequestStatus.OPEN) {
    res.status(400).json({ error: "Request is already closed" });
    return;
  }

  const winnerBid = winnerId ? request.bids.find((b) => b.brokerId === winnerId) : null;

  // Refund all losing bidders in one transaction
  await prisma.$transaction(async (tx) => {
    for (const bid of request.bids) {
      const isWinner = winnerBid?.brokerId === bid.brokerId;
      if (!isWinner) {
        // Refund loser
        const user = await tx.user.findUniqueOrThrow({ where: { id: bid.brokerId }, select: { tokenBalance: true } });
        const refundedBalance = user.tokenBalance + bid.tokens;
        await tx.user.update({
          where: { id: bid.brokerId },
          data: {
            tokenBalance:     refundedBalance,
            totalTokensSpent: { decrement: bid.tokens },
          },
        });
        await tx.tokenTransaction.create({
          data: {
            userId:          bid.brokerId,
            type:            "EARN",
            amount:          bid.tokens,
            reason:          "BOOST_LISTING",
            balanceAfter:    refundedBalance,
            relatedEntityId: id,
          },
        });
      }
      await tx.bid.update({
        where: { id: bid.id },
        data:  { isWinner: bid.brokerId === winnerBid?.brokerId },
      });
    }

    await tx.buyerRequest.update({
      where: { id },
      data:  { status: RequestStatus.CLOSED },
    });
  });

  // Return winner's contact info if there's a winner
  if (winnerBid) {
    const winner = await prisma.user.findUnique({
      where:  { id: winnerBid.brokerId },
      select: { id: true, fullName: true },
    });
    res.json({ closed: true, winner });
  } else {
    res.json({ closed: true, winner: null });
  }
}
