import { Request, Response } from "express";
import { z } from "zod";
import prisma from "../utils/prisma.js";
import type { AuthenticatedRequest } from "../middleware/auth.js";
import { OfficerStatus, UserType } from "@prisma/client";

const updateProfileSchema = z.object({
  fullName: z.string().min(2).max(100).optional(),
  governorate: z.string().optional(),
  photoUrl: z.string().url().optional(),
  phoneNumber: z.string().max(20).optional(),
});

const officerProfileSchema = z.object({
  rank: z.string().min(1),
  status: z.nativeEnum(OfficerStatus),
  documentUrl: z.string().min(1),
});

export async function getMe(req: AuthenticatedRequest, res: Response): Promise<void> {
  const user = await prisma.user.findUnique({
    where: { id: req.user!.userId },
    select: {
      id: true,
      fullName: true,
      governorate: true,
      photoUrl: true,
      userType: true,
      createdAt: true,
      phoneNumber: true,
      tokenBalance: true,
      totalTokensEarned: true,
      totalTokensSpent: true,
      referralCode: true,
      officerProfile: {
        select: {
          rank: true,
          status: true,
          verificationState: true,
          exemptionUsed: true,
        },
      },
    },
  });

  if (!user) {
    res.status(404).json({ error: "User not found" });
    return;
  }

  res.json(user);
}

export async function updateMe(req: AuthenticatedRequest, res: Response): Promise<void> {
  const parsed = updateProfileSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const user = await prisma.user.update({
    where: { id: req.user!.userId },
    data: parsed.data,
    select: { id: true, fullName: true, governorate: true, photoUrl: true, userType: true, phoneNumber: true },
  });

  res.json(user);
}

export async function updateFcmToken(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { token } = req.body as { token?: string };
  if (!token || typeof token !== "string") {
    res.status(400).json({ error: "token required" });
    return;
  }

  await prisma.user.update({
    where: { id: req.user!.userId },
    data: { fcmToken: token },
  });

  res.json({ ok: true });
}

export async function deleteMe(req: AuthenticatedRequest, res: Response): Promise<void> {
  await prisma.user.update({
    where: { id: req.user!.userId },
    data: { isActive: false },
  });

  res.json({ message: "Account deletion scheduled" });
}

export async function submitOfficerProfile(req: AuthenticatedRequest, res: Response): Promise<void> {
  const parsed = officerProfileSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const existing = await prisma.officerProfile.findUnique({
    where: { userId: req.user!.userId },
  });

  if (existing) {
    res.status(409).json({ error: "Officer profile already submitted" });
    return;
  }

  const profile = await prisma.officerProfile.create({
    data: {
      userId: req.user!.userId,
      rank: parsed.data.rank,
      status: parsed.data.status,
      documentUrl: parsed.data.documentUrl,
    },
    select: { verificationState: true, rank: true, status: true },
  });

  res.status(201).json(profile);
}

const medicalExemptSchema = z.object({
  documentUrl: z.string().min(5),
});

export async function submitMedicalExemptProfile(req: AuthenticatedRequest, res: Response): Promise<void> {
  if (req.user!.userType !== UserType.MEDICAL_EXEMPT) {
    res.status(403).json({ error: "Only MEDICAL_EXEMPT users can submit this profile" });
    return;
  }

  const parsed = medicalExemptSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const existing = await prisma.medicalExemptProfile.findUnique({
    where: { userId: req.user!.userId },
  });

  if (existing) {
    res.status(409).json({ error: "Medical exempt profile already submitted" });
    return;
  }

  const profile = await prisma.medicalExemptProfile.create({
    data: {
      userId: req.user!.userId,
      documentUrl: parsed.data.documentUrl,
    },
    select: { verificationState: true, verifiedAt: true },
  });

  res.status(201).json(profile);
}

export async function getOfficerProfileStatus(req: AuthenticatedRequest, res: Response): Promise<void> {
  const profile = await prisma.officerProfile.findUnique({
    where: { userId: req.user!.userId },
    select: { verificationState: true, rejectionReason: true, verifiedAt: true },
  });

  if (!profile) {
    res.status(404).json({ error: "Officer profile not found" });
    return;
  }

  res.json(profile);
}

