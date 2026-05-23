import { Request, Response } from "express";
import { z } from "zod";
import prisma from "../utils/prisma.js";
import { applyTokens } from "../utils/tokens.js";
import type { AuthenticatedRequest } from "../middleware/auth.js";
import { ListingType, ListingCategory, ListingStatus, TxReason, Prisma } from "@prisma/client";

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
  listingCategory: z.nativeEnum(ListingCategory).optional(),
  sellerType: z.string().optional(),
  restrictionEndsAt: z.string().datetime().optional(),
  originalPrice: z.number().int().positive().optional(),
  governorate: z.string().min(1),
  notes: z.string().max(1000).optional(),
  photos: z.array(z.string().url()).max(10).optional(),
});

export async function getListings(req: Request, res: Response): Promise<void> {
  const { governorate, type, category, minPrice, maxPrice, sort, page = "1", limit = "20" } = req.query as Record<string, string>;

  const pageNum = Math.max(1, parseInt(page));
  const limitNum = Math.min(50, Math.max(1, parseInt(limit)));

  // Prisma where for the count query
  const where = {
    status: ListingStatus.ACTIVE,
    ...(governorate ? { governorate } : {}),
    ...(type ? { listingType: type as ListingType } : {}),
    ...(category ? { listingCategory: category as ListingCategory } : {}),
    ...(minPrice || maxPrice
      ? {
          expectedPrice: {
            ...(minPrice ? { gte: parseInt(minPrice) } : {}),
            ...(maxPrice ? { lte: parseInt(maxPrice) } : {}),
          },
        }
      : {}),
  };

  // Dynamic WHERE for raw query
  const conditions: Prisma.Sql[] = [Prisma.sql`l.status = 'ACTIVE'::"ListingStatus"`];
  if (governorate) conditions.push(Prisma.sql`l.governorate = ${governorate}`);
  if (type)        conditions.push(Prisma.sql`l."listingType" = ${type}::"ListingType"`);
  if (category)    conditions.push(Prisma.sql`l."listingCategory" = ${category}::"ListingCategory"`);
  if (minPrice)    conditions.push(Prisma.sql`l."expectedPrice" >= ${parseInt(minPrice)}`);
  if (maxPrice)    conditions.push(Prisma.sql`l."expectedPrice" <= ${parseInt(maxPrice)}`);
  const whereClause = Prisma.join(conditions, " AND ");

  const secondaryOrder =
    sort === "price_asc"  ? Prisma.sql`l."expectedPrice" ASC NULLS LAST` :
    sort === "price_desc" ? Prisma.sql`l."expectedPrice" DESC NULLS LAST` :
                            Prisma.sql`l."createdAt" DESC`;

  // Raw query: CASE WHEN tier priority respecting tierExpiresAt, then secondary sort
  const rows = await prisma.$queryRaw<Array<Record<string, unknown>>>`
    SELECT
      l.id,
      l."vehicleType",
      l."makeModel",
      l."yearMin",
      l."yearMax",
      l.color,
      l."mileageKm",
      l."fuelType",
      l.transmission,
      l."marketPrice",
      l."expectedPrice",
      l."listingType",
      l."listingCategory"::text AS "listingCategory",
      l."sellerType",
      l."restrictionEndsAt",
      l."originalPrice",
      l.tier::text          AS tier,
      l."tierExpiresAt",
      l.governorate,
      l.photos,
      l.notes,
      l."createdAt",
      CASE
        WHEN l.tier != 'FREE'::"ListingTier"
          AND (l."tierExpiresAt" IS NULL OR l."tierExpiresAt" > NOW())
        THEN CASE l.tier::text
          WHEN 'GOLD'     THEN 1
          WHEN 'VIP'      THEN 2
          WHEN 'FEATURED' THEN 3
          ELSE 4
        END
        ELSE 4
      END AS "_tierPri",
      CASE WHEN u.id IS NOT NULL
        THEN json_build_object(
          'id',          u.id,
          'fullName',    u."fullName",
          'officerProfile', CASE WHEN op.id IS NOT NULL
            THEN json_build_object(
              'verificationState', op."verificationState",
              'rank',              op.rank
            )
            ELSE NULL
          END
        )
        ELSE NULL
      END AS officer
    FROM "Listing" l
    LEFT JOIN "User"           u  ON l."officerId" = u.id
    LEFT JOIN "OfficerProfile" op ON op."userId"   = u.id
    WHERE ${whereClause}
    ORDER BY "_tierPri" ASC, ${secondaryOrder}
    LIMIT  ${limitNum}
    OFFSET ${(pageNum - 1) * limitNum}
  `;

  // Strip internal sort helper before sending to client
  const listings = rows.map(({ _tierPri, ...rest }) => rest);

  const total = await prisma.listing.count({ where });
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
  const parsed = createListingSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const category = parsed.data.listingCategory ?? ListingCategory.MORTAJA3;

  const userProfiles = await prisma.user.findUnique({
    where: { id: req.user!.userId },
    select: { officerProfile: { select: { id: true } } },
  });

  const sellerType = parsed.data.sellerType ??
    (userProfiles?.officerProfile ? "OFFICER" : undefined);

  const listing = await prisma.listing.create({
    data: {
      ...parsed.data,
      listingCategory: category,
      sellerType,
      officerId: req.user!.userId,
    },
  });

  const txReason = category === ListingCategory.EXEMPTION_RIGHT
    ? TxReason.POST_EXEMPTION
    : TxReason.POST_LISTING;

  try {
    await applyTokens(req.user!.userId, txReason, listing.id);
  } catch (err: unknown) {
    await prisma.listing.delete({ where: { id: listing.id } });
    if (err instanceof Error && err.message.startsWith("INSUFFICIENT_TOKENS")) {
      const current = err.message.split(":")[1];
      res.status(402).json({ error: "insufficient_tokens", balance: Number(current) });
      return;
    }
    throw err;
  }

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
    select: { phoneNumber: true, status: true, listingCategory: true },
  });

  if (!listing || listing.status === ListingStatus.REMOVED) {
    res.status(404).json({ error: "Listing not found" });
    return;
  }

  if (!listing.phoneNumber) {
    res.json({ phoneNumber: "غير متوفر", charged: false });
    return;
  }

  try {
    await applyTokens(req.user!.userId, TxReason.REVEAL_CONTACT, id);
    res.json({ phoneNumber: listing.phoneNumber, charged: true });
  } catch (err: unknown) {
    if (err instanceof Error && err.message.startsWith("INSUFFICIENT_TOKENS")) {
      const current = err.message.split(":")[1];
      res.status(402).json({ error: "INSUFFICIENT_TOKENS", currentBalance: Number(current) });
    } else {
      res.status(500).json({ error: "خطأ غير متوقع" });
    }
  }
}
