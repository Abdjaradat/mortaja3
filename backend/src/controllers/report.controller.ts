import { Response } from "express";
import { z } from "zod";
import prisma from "../utils/prisma.js";
import type { AuthenticatedRequest } from "../middleware/auth.js";

const createReportSchema = z.object({
  targetType: z.enum(["USER", "LISTING", "MESSAGE"]),
  targetId: z.string().cuid(),
  reason: z.string().min(1).max(200),
  description: z.string().max(1000).optional(),
});

export async function createReport(req: AuthenticatedRequest, res: Response): Promise<void> {
  const parsed = createReportSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const report = await prisma.report.create({
    data: { reporterId: req.user!.userId, ...parsed.data },
    select: { id: true, status: true, createdAt: true },
  });

  res.status(201).json(report);
}
