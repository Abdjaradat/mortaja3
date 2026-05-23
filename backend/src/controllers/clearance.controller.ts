import { Request, Response } from "express";
import { z } from "zod";
import prisma from "../utils/prisma.js";
import { applyTokens } from "../utils/tokens.js";
import type { AuthenticatedRequest } from "../middleware/auth.js";
import { ClearanceLocation, ClearanceStatus } from "@prisma/client";

// ─── Schemas ─────────────────────────────────────────────────────────────────

const registerAgentSchema = z.object({
  displayName:     z.string().min(2).max(100),
  location:        z.enum(["BOHRET_AMMAN", "ZARQA"]),
  specializations: z.array(z.enum(["CARS", "GOODS", "CONTAINERS", "ALL"])).min(1),
  yearsExperience: z.number().int().min(0).max(50),
  bio:             z.string().max(500).optional(),
});

const createRequestSchema = z.object({
  serviceType:  z.enum(["CARS", "GOODS", "CONTAINERS", "OTHER"]),
  location:     z.enum(["BOHRET_AMMAN", "ZARQA"]),
  description:  z.string().min(10).max(1000),
  budgetMax:    z.number().int().positive().optional(),
});

const offerSchema = z.object({
  price: z.number().int().positive(),
  notes: z.string().max(300).optional(),
});

const selectSchema = z.object({
  agentId: z.string().min(1),
});

const rateSchema = z.object({
  score: z.number().int().min(1).max(5),
});

// ─── Selects ─────────────────────────────────────────────────────────────────

const agentSelect = {
  id:              true,
  userId:          true,
  displayName:     true,
  photoUrl:        true,
  location:        true,
  specializations: true,
  yearsExperience: true,
  bio:             true,
  isVerified:      true,
  totalDeals:      true,
  avgRating:       true,
  ratingCount:     true,
  createdAt:       true,
} as const;

const offerSelect = {
  id:         true,
  agentId:    true,
  price:      true,
  notes:      true,
  isSelected: true,
  placedAt:   true,
  agent:      { select: agentSelect },
} as const;

const requestSelect = {
  id:          true,
  customerId:  true,
  serviceType: true,
  location:    true,
  description: true,
  budgetMax:   true,
  status:      true,
  isRated:     true,
  expiresAt:   true,
  createdAt:   true,
  customer:    { select: { id: true, fullName: true } },
  offers:      { select: offerSelect, orderBy: { price: "asc" as const } },
} as const;

// ─── Helpers ─────────────────────────────────────────────────────────────────

function buildRequestResponse(request: any, viewerUserId: string) {
  const LOCK_MS = 4 * 60 * 60 * 1000;
  const offersHidden = Date.now() - new Date(request.createdAt).getTime() < LOCK_MS;
  const offers = offersHidden
    ? request.offers.filter((o: any) => o.agent.userId === viewerUserId)
    : request.offers;
  return { ...request, offersHidden, offers };
}

// ─── Agent endpoints ──────────────────────────────────────────────────────────

// POST /api/v1/clearance/agents
export async function registerAgent(req: AuthenticatedRequest, res: Response): Promise<void> {
  const userId = req.user!.userId;

  const existing = await prisma.clearanceAgent.findUnique({ where: { userId }, select: { id: true } });
  if (existing) {
    res.status(409).json({ error: "already_registered" });
    return;
  }

  const parsed = registerAgentSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  try {
    await applyTokens(userId, "REGISTER_CLEARANCE_AGENT");
  } catch (err: unknown) {
    if (err instanceof Error && err.message.startsWith("INSUFFICIENT_TOKENS")) {
      const current = err.message.split(":")[1];
      res.status(402).json({ error: "insufficient_tokens", balance: Number(current) });
      return;
    }
    throw err;
  }

  const agent = await prisma.clearanceAgent.create({
    data: { ...parsed.data, userId },
    select: agentSelect,
  });

  res.status(201).json(agent);
}

// GET /api/v1/clearance/agents
export async function listAgents(req: Request, res: Response): Promise<void> {
  const location = req.query["location"] as string | undefined;
  const where = location ? { location: location as ClearanceLocation } : {};

  const agents = await prisma.clearanceAgent.findMany({
    where,
    select: agentSelect,
    orderBy: [{ avgRating: "desc" }, { totalDeals: "desc" }],
  });

  res.json(agents);
}

// GET /api/v1/clearance/agents/me
export async function getMyAgent(req: AuthenticatedRequest, res: Response): Promise<void> {
  const userId = req.user!.userId;
  const agent = await prisma.clearanceAgent.findUnique({ where: { userId }, select: agentSelect });
  if (!agent) {
    res.status(404).json({ error: "not_an_agent" });
    return;
  }
  res.json(agent);
}

// GET /api/v1/clearance/agents/:id
export async function getAgentById(req: Request, res: Response): Promise<void> {
  const { id } = req.params as { id: string };
  const agent = await prisma.clearanceAgent.findUnique({ where: { id }, select: agentSelect });
  if (!agent) {
    res.status(404).json({ error: "Agent not found" });
    return;
  }
  res.json(agent);
}

// ─── Request endpoints ────────────────────────────────────────────────────────

// POST /api/v1/clearance/requests
export async function createRequest(req: AuthenticatedRequest, res: Response): Promise<void> {
  const parsed = createRequestSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000);
  const request = await prisma.clearanceRequest.create({
    data: { ...parsed.data, customerId: req.user!.userId, expiresAt },
    select: requestSelect,
  });

  res.status(201).json(buildRequestResponse(request, req.user!.userId));
}

// GET /api/v1/clearance/requests
export async function listRequests(req: AuthenticatedRequest, res: Response): Promise<void> {
  const userId  = req.user!.userId;
  const isMine  = req.query["mine"] === "true";
  const now     = new Date();

  const where = isMine
    ? { customerId: userId }
    : { status: ClearanceStatus.OPEN, expiresAt: { gt: now } };

  const requests = await prisma.clearanceRequest.findMany({
    where,
    select:  requestSelect,
    orderBy: { createdAt: "desc" },
  });

  res.json(requests.map((r) => buildRequestResponse(r, userId)));
}

// GET /api/v1/clearance/requests/:id
export async function getRequestById(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id } = req.params as { id: string };
  const request = await prisma.clearanceRequest.findUnique({ where: { id }, select: requestSelect });

  if (!request) {
    res.status(404).json({ error: "Request not found" });
    return;
  }

  res.json(buildRequestResponse(request, req.user!.userId));
}

// POST /api/v1/clearance/requests/:id/offer
export async function submitOffer(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id }   = req.params as { id: string };
  const userId   = req.user!.userId;

  const agent = await prisma.clearanceAgent.findUnique({ where: { userId }, select: { id: true } });
  if (!agent) {
    res.status(403).json({ error: "not_an_agent" });
    return;
  }

  const parsed = offerSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const request = await prisma.clearanceRequest.findUnique({
    where:  { id },
    select: { id: true, status: true, expiresAt: true, customerId: true,
              offers: { where: { agentId: agent.id }, select: { id: true, price: true } } },
  });

  if (!request || request.status !== ClearanceStatus.OPEN || request.expiresAt < new Date()) {
    res.status(404).json({ error: "Request not found or closed" });
    return;
  }
  if (request.customerId === userId) {
    res.status(400).json({ error: "Cannot offer on your own request" });
    return;
  }

  const existingOffer = request.offers[0] ?? null;

  try {
    const offer = await prisma.$transaction(async (tx) => {
      if (existingOffer) {
        // Refund previous offer tokens then deduct new ones
        const user = await tx.user.findUniqueOrThrow({ where: { id: userId }, select: { tokenBalance: true } });
        const refunded = user.tokenBalance + 10; // refund old 10-token offer
        await tx.user.update({
          where: { id: userId },
          data:  { tokenBalance: refunded, totalTokensSpent: { decrement: 10 } },
        });
        await tx.tokenTransaction.create({
          data: { userId, type: "EARN", amount: 10, reason: "CLEARANCE_OFFER", balanceAfter: refunded, relatedEntityId: id },
        });
        await tx.clearanceOffer.delete({ where: { id: existingOffer.id } });
      }

      // Deduct 10 tokens for new offer
      const user = await tx.user.findUniqueOrThrow({ where: { id: userId }, select: { tokenBalance: true } });
      const newBalance = user.tokenBalance - 10;
      if (newBalance < 0) throw new Error(`INSUFFICIENT_TOKENS:${user.tokenBalance}`);
      await tx.user.update({
        where: { id: userId },
        data:  { tokenBalance: newBalance, totalTokensSpent: { increment: 10 } },
      });
      await tx.tokenTransaction.create({
        data: { userId, type: "SPEND", amount: -10, reason: "CLEARANCE_OFFER", balanceAfter: newBalance, relatedEntityId: id },
      });

      return tx.clearanceOffer.create({
        data:   { requestId: id, agentId: agent.id, ...parsed.data },
        select: offerSelect,
      });
    });

    res.status(201).json(offer);
  } catch (err: unknown) {
    if (err instanceof Error && err.message.startsWith("INSUFFICIENT_TOKENS")) {
      const current = err.message.split(":")[1];
      res.status(402).json({ error: "insufficient_tokens", balance: Number(current) });
    } else {
      throw err;
    }
  }
}

// POST /api/v1/clearance/requests/:id/select
export async function selectAgent(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id }  = req.params as { id: string };
  const userId  = req.user!.userId;

  const parsed = selectSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }
  const { agentId } = parsed.data;

  const request = await prisma.clearanceRequest.findUnique({
    where:  { id },
    select: { id: true, status: true, customerId: true,
              offers: { select: { id: true, agentId: true, agent: { select: { id: true, userId: true, totalDeals: true } } } } },
  });

  if (!request || request.customerId !== userId) {
    res.status(404).json({ error: "Request not found" });
    return;
  }
  if (request.status !== ClearanceStatus.OPEN) {
    res.status(400).json({ error: "Request already closed" });
    return;
  }

  const winnerOffer = request.offers.find((o) => o.agentId === agentId);
  if (!winnerOffer) {
    res.status(404).json({ error: "Agent has no offer on this request" });
    return;
  }

  try {
    await prisma.$transaction(async (tx) => {
      // Deduct 30 tokens from winning agent's user account
      const winnerUserId = winnerOffer.agent.userId;
      const winnerUser = await tx.user.findUniqueOrThrow({ where: { id: winnerUserId }, select: { tokenBalance: true } });
      const winnerNewBalance = winnerUser.tokenBalance - 30;
      if (winnerNewBalance < 0) throw new Error(`INSUFFICIENT_TOKENS:${winnerUser.tokenBalance}`);
      await tx.user.update({
        where: { id: winnerUserId },
        data:  { tokenBalance: winnerNewBalance, totalTokensSpent: { increment: 30 } },
      });
      await tx.tokenTransaction.create({
        data: { userId: winnerUserId, type: "SPEND", amount: -30, reason: "CLEARANCE_SELECT", balanceAfter: winnerNewBalance, relatedEntityId: id },
      });

      // Refund 10 tokens to all LOSING agents
      for (const offer of request.offers) {
        if (offer.agentId === agentId) continue;
        const loserUserId = offer.agent.userId;
        const loserUser = await tx.user.findUniqueOrThrow({ where: { id: loserUserId }, select: { tokenBalance: true } });
        const refunded = loserUser.tokenBalance + 10;
        await tx.user.update({
          where: { id: loserUserId },
          data:  { tokenBalance: refunded, totalTokensSpent: { decrement: 10 } },
        });
        await tx.tokenTransaction.create({
          data: { userId: loserUserId, type: "EARN", amount: 10, reason: "CLEARANCE_OFFER", balanceAfter: refunded, relatedEntityId: id },
        });
        await tx.clearanceOffer.update({ where: { id: offer.id }, data: { isSelected: false } });
      }

      // Mark winner offer
      await tx.clearanceOffer.update({ where: { id: winnerOffer.id }, data: { isSelected: true } });

      // Update agent deal count + close request
      await tx.clearanceAgent.update({
        where: { id: agentId },
        data:  { totalDeals: { increment: 1 } },
      });
      await tx.clearanceRequest.update({ where: { id }, data: { status: ClearanceStatus.CLOSED } });
    });

    const winner = await prisma.clearanceAgent.findUnique({ where: { id: agentId }, select: agentSelect });
    res.json({ closed: true, winner });
  } catch (err: unknown) {
    if (err instanceof Error && err.message.startsWith("INSUFFICIENT_TOKENS")) {
      const current = err.message.split(":")[1];
      res.status(402).json({ error: "insufficient_tokens", balance: Number(current) });
    } else {
      throw err;
    }
  }
}

// POST /api/v1/clearance/requests/:id/rate
export async function rateAgent(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id }  = req.params as { id: string };
  const userId  = req.user!.userId;

  const parsed = rateSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }
  const { score } = parsed.data;

  const request = await prisma.clearanceRequest.findUnique({
    where:  { id },
    select: { id: true, customerId: true, status: true, isRated: true,
              offers: { where: { isSelected: true }, select: { agentId: true, agent: { select: { avgRating: true, ratingCount: true } } } } },
  });

  if (!request || request.customerId !== userId) {
    res.status(404).json({ error: "Request not found" });
    return;
  }
  if (request.status !== ClearanceStatus.CLOSED) {
    res.status(400).json({ error: "Request not yet closed" });
    return;
  }
  if (request.isRated) {
    res.status(409).json({ error: "Already rated" });
    return;
  }

  const winnerOffer = request.offers[0];
  if (!winnerOffer) {
    res.status(400).json({ error: "No winner selected" });
    return;
  }

  const { agentId, agent } = winnerOffer;
  const newRatingCount = agent.ratingCount + 1;
  const newAvgRating = (agent.avgRating * agent.ratingCount + score) / newRatingCount;

  await prisma.$transaction(async (tx) => {
    await tx.clearanceAgent.update({
      where: { id: agentId },
      data:  { avgRating: newAvgRating, ratingCount: newRatingCount },
    });
    await tx.clearanceRequest.update({ where: { id }, data: { isRated: true } });
  });

  // Bonus tokens to the customer for rating
  await applyTokens(userId, "CLEARANCE_RATE_BONUS", id);

  res.json({ rated: true, tokensEarned: 20 });
}
