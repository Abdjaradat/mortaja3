import { Request, Response } from "express";
import { z } from "zod";
import prisma from "../utils/prisma.js";
import { applyTokens } from "../utils/tokens.js";
import type { AuthenticatedRequest } from "../middleware/auth.js";
import { ListingType, ListingStatus, TxReason } from "@prisma/client";

const createListingSchema = z.object({
  vehicleType: z.enum(["SEDAN", "SUV", "HYBRID", "EV", "OTHER"]),
  makeModel: z.string().min(1).max(100),
  yearMin: z.number().int().min(2000).max(2030).optional(),
  yearMax: z.number().int().min(2000).max(2030).optional(),
  color: z.string().max(50).optional(),
  mileageKm: z.number().int().min(0).max(1_000_000).optional(),
  fuelType: z.enum(["GASOLINE", "DIESEL", "HYBRID", "ELECTRIC"]).optional(),
  transmission: z.enum(["AUTOMATIC", "MANUAL"]).optional(),
  phoneNumber: z.string().max(20).optional(),
  marketPrice: z.number().int().positive().optional(),
  expectedPrice: z.number().int().positive().optional(),
  listingType: z.nativeEnum(ListingType),
  governorate: z.string().min(1),
  notes: z.string().max(1000).optional(),
  photos: z.array(z.string().url()).max(10).optional(),
});

export async function getListings(req: Request, res: Response): Promise<void> {
  const { governorate, type, minPrice, maxPrice, sort, page = "1", limit = "20" } = req.query as Record<string, string>;

  const pageNum = Math.max(1, parseInt(page));
  const limitNum = Math.min(50, Math.max(1, parseInt(limit)));

  const where = {
    status: ListingStatus.ACTIVE,
    ...(governorate ? { governorate } : {}),
    ...(type ? { listingType: type as ListingType } : {}),
    ...(minPrice || maxPrice
      ? {
          expectedPrice: {
            ...(minPrice ? { gte: parseInt(minPrice) } : {}),
            ...(maxPrice ? { lte: parseInt(maxPrice) } : {}),
          },
        }
      : {}),
  };

  const orderBy =
    sort === "price_asc"
      ? { expectedPrice: "asc" as const }
      : sort === "price_desc"
      ? { expectedPrice: "desc" as const }
      : { createdAt: "desc" as const };

  const [listings, total] = await Promise.all([
    prisma.listing.findMany({
      where,
      orderBy,
      skip: (pageNum - 1) * limitNum,
      take: limitNum,
      select: {
        id: true,
        vehicleType: true,
        makeModel: true,
        yearMin: true,
        yearMax: true,
        color: true,
        mileageKm: true,
        fuelType: true,
        transmission: true,
        marketPrice: true,
        expectedPrice: true,
        listingType: true,
        tier: true,
        tierExpiresAt: true,
        governorate: true,
        photos: true,
        notes: true,
        createdAt: true,
        officer: {
          select: {
            id: true,
            fullName: true,
            officerProfile: { select: { verificationState: true, rank: true } },
          },
        },
      },
    }),
    prisma.listing.count({ where }),
  ]);

  res.json({ listings, total, page: pageNum, limit: limitNum });
}

export async function getListingById(req: Request, res: Response): Promise<void> {
  const { id } = req.params as { id: string };
  const listing = await prisma.listing.findUnique({
    where: { id },
    include: {
      officer: {
        select: {
          id: true,
          fullName: true,
          governorate: true,
          officerProfile: { select: { verificationState: true, rank: true } },
        },
      },
    },
  });

  if (!listing || listing.status === ListingStatus.REMOVED) {
    res.status(404).json({ error: "Listing not found" });
    return;
  }

  await prisma.listing.update({
    where: { id: listing.id },
    data: { viewCount: { increment: 1 } },
  });

  res.json(listing);
}

export async function createListing(req: AuthenticatedRequest, res: Response): Promise<void> {
  const profile = await prisma.officerProfile.findUnique({
    where: { userId: req.user!.userId },
  });

  if (!profile || profile.verificationState !== "VERIFIED") {
    res.status(403).json({ error: "Officer must be verified to post listings" });
    return;
  }

  if (profile.exemptionUsed) {
    res.status(400).json({ error: "Exemption has already been used" });
    return;
  }

  const parsed = createListingSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const listing = await prisma.listing.create({
    data: { ...parsed.data, officerId: req.user!.userId },
  });

  res.status(201).json(listing);
}

export async function updateListing(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id } = req.params as { id: string };
  const listing = await prisma.listing.findUnique({ where: { id } });

  if (!listing || listing.officerId !== req.user!.userId) {
    res.status(404).json({ error: "Listing not found" });
    return;
  }

  const parsed = createListingSchema.partial().safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const updated = await prisma.listing.update({
    where: { id: listing.id },
    data: parsed.data,
  });

  res.json(updated);
}

export async function deleteListing(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id } = req.params as { id: string };
  const listing = await prisma.listing.findUnique({ where: { id } });

  if (!listing || listing.officerId !== req.user!.userId) {
    res.status(404).json({ error: "Listing not found" });
    return;
  }

  await prisma.listing.update({
    where: { id: listing.id },
    data: { status: ListingStatus.REMOVED },
  });

  res.status(204).send();
}

export async function saveListing(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id } = req.params as { id: string };
  const listing = await prisma.listing.findUnique({ where: { id } });
  if (!listing) {
    res.status(404).json({ error: "Listing not found" });
    return;
  }

  await prisma.savedListing.upsert({
    where: { userId_listingId: { userId: req.user!.userId, listingId: id } },
    update: {},
    create: { userId: req.user!.userId, listingId: listing.id },
  });

  res.status(201).json({ saved: true });
}

export async function unsaveListing(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id } = req.params as { id: string };
  await prisma.savedListing.deleteMany({
    where: { userId: req.user!.userId, listingId: id },
  });

  res.status(204).send();
}

export async function getSavedListings(req: AuthenticatedRequest, res: Response): Promise<void> {
  const saved = await prisma.savedListing.findMany({
    where: { userId: req.user!.userId },
    include: {
      listing: {
        select: {
          id: true,
          vehicleType: true,
          makeModel: true,
          expectedPrice: true,
          governorate: true,
          status: true,
          photos: true,
        },
      },
    },
    orderBy: { createdAt: "desc" },
  });

  res.json(saved.map((s) => s.listing));
}

export async function revealContact(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id } = req.params as { id: string };

  const listing = await prisma.listing.findUnique({
    where: { id },
    select: { phoneNumber: true, status: true },
  });

  if (!listing || listing.status === ListingStatus.REMOVED) {
    res.status(404).json({ error: "Listing not found" });
    return;
  }

  if (!listing.phoneNumber) {
    res.status(404).json({ error: "No phone number available" });
    return;
  }

  try {
    await applyTokens(req.user!.userId, TxReason.REVEAL_CONTACT, id);
    res.json({ phoneNumber: listing.phoneNumber });
  } catch (err: unknown) {
    if (err instanceof Error && err.message.startsWith("INSUFFICIENT_TOKENS")) {
      const current = err.message.split(":")[1];
      res.status(402).json({ error: "INSUFFICIENT_TOKENS", currentBalance: Number(current) });
    } else {
      res.status(500).json({ error: "خطأ غير متوقع" });
    }
  }
}
