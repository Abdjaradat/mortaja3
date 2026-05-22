import { Request, Response } from "express";
import { z } from "zod";
import prisma from "../utils/prisma.js";
import { sendPushNotification } from "../utils/firebase.js";

export async function getPendingVerifications(_req: Request, res: Response): Promise<void> {
  const profiles = await prisma.officerProfile.findMany({
    where: { verificationState: "PENDING" },
    include: {
      user: { select: { id: true, fullName: true } },
    },
    orderBy: { createdAt: "asc" },
  });

  res.json(profiles);
}

export async function approveVerification(req: Request, res: Response): Promise<void> {
  const { id } = req.params as { id: string };

  const profile = await prisma.officerProfile.findUnique({
    where: { id },
    include: { user: { select: { fcmToken: true } } },
  });
  if (!profile) {
    res.status(404).json({ error: "Profile not found" });
    return;
  }

  await prisma.officerProfile.update({
    where: { id },
    data: { verificationState: "VERIFIED", verifiedAt: new Date(), rejectionReason: null },
  });

  if (profile.user.fcmToken) {
    await sendPushNotification(
      profile.user.fcmToken,
      "تم توثيق حسابك ✓",
      "تهانينا! تم مراجعة وثائقك والتحقق من هويتك. يمكنك الآن نشر إعلانات.",
      { type: "VERIFICATION_APPROVED" },
    );
  }

  res.json({ message: "Approved" });
}

export async function rejectVerification(req: Request, res: Response): Promise<void> {
  const { id } = req.params as { id: string };
  const { reason } = req.body as { reason?: string };

  if (!reason) {
    res.status(400).json({ error: "Rejection reason required" });
    return;
  }

  const profile = await prisma.officerProfile.findUnique({
    where: { id },
    include: { user: { select: { fcmToken: true } } },
  });
  if (!profile) {
    res.status(404).json({ error: "Profile not found" });
    return;
  }

  await prisma.officerProfile.update({
    where: { id },
    data: { verificationState: "REJECTED", rejectionReason: reason },
  });

  if (profile.user.fcmToken) {
    await sendPushNotification(
      profile.user.fcmToken,
      "طلب التوثيق مرفوض",
      reason,
      { type: "VERIFICATION_REJECTED" },
    );
  }

  res.json({ message: "Rejected" });
}

export async function getReports(_req: Request, res: Response): Promise<void> {
  const reports = await prisma.report.findMany({
    where: { status: "OPEN" },
    orderBy: { createdAt: "asc" },
  });

  res.json(reports);
}

export async function blockUser(req: Request, res: Response): Promise<void> {
  const { id } = req.params as { id: string };

  const user = await prisma.user.findUnique({ where: { id } });
  if (!user) {
    res.status(404).json({ error: "User not found" });
    return;
  }

  await prisma.user.update({ where: { id }, data: { isBlocked: true } });
  res.json({ message: "User blocked" });
}
