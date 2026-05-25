import { Request, Response } from "express";
import { z } from "zod";
import prisma from "../utils/prisma.js";
import { verifyFirebaseIdToken } from "../utils/firebase.js";
import { signAccessToken, signRefreshToken, verifyToken, buildTokenPayload } from "../utils/jwt.js";
import { UserType, TxType, TxReason } from "@prisma/client";
import { generateReferralCode } from "../utils/tokens.js";

const verifyFirebaseTokenSchema = z.object({
  idToken: z.string().min(1),
  userType: z.nativeEnum(UserType).optional(),
});

export async function verifyOtp(req: Request, res: Response): Promise<void> {
  const parsed = verifyFirebaseTokenSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const { idToken, userType } = parsed.data;

  let decoded;
  try {
    decoded = await verifyFirebaseIdToken(idToken);
  } catch {
    res.status(401).json({ error: "Invalid Firebase ID token" });
    return;
  }

  const firebaseUid = decoded.uid;
  const email = decoded.email ?? null;

  try {
    let user = await prisma.user.findUnique({ where: { firebaseUid } });
    let isNewUser = !user;

    if (!user) {
      // Check if email already exists — happens when user registered with email/password
      // and now signs in with Google using the same email (different firebaseUid).
      if (email) {
        const byEmail = await prisma.user.findUnique({ where: { email } });
        if (byEmail) {
          // Link the new Firebase UID to the existing account and sign them in.
          user = await prisma.user.update({
            where: { id: byEmail.id },
            data: { firebaseUid },
          });
          isNewUser = false;
        }
      }

      if (!user) {
        // Completely new user — client must provide userType on first call.
        if (!userType) {
          res.status(400).json({ error: "userType required for first registration" });
          return;
        }
        user = await prisma.user.create({
          data: { firebaseUid, email, userType, referralCode: generateReferralCode() },
        });
        await prisma.tokenTransaction.create({
          data: {
            userId: user.id,
            type: TxType.EARN,
            amount: 500,
            reason: TxReason.WELCOME,
            balanceAfter: 500,
          },
        });
      }
    } else if (email && user.email !== email) {
      user = await prisma.user.update({
        where: { firebaseUid },
        data: { email },
      });
    }

    if (user.isBlocked) {
      res.status(403).json({ error: "Account is blocked" });
      return;
    }

    const payload = buildTokenPayload(user.id, user.userType);

    res.json({
      accessToken: signAccessToken(payload),
      refreshToken: signRefreshToken(payload),
      user: {
        id: user.id,
        userType: user.userType,
        fullName: user.fullName,
        isNewUser,
      },
    });
  } catch (error: unknown) {
    // P2002 race condition: two concurrent requests tried to create the same user.
    // Recover by finding the existing row and linking the Firebase UID.
    if (
      error instanceof Error &&
      "code" in error &&
      (error as { code: string }).code === "P2002" &&
      email
    ) {
      try {
        const byEmail = await prisma.user.findUnique({ where: { email } });
        if (byEmail) {
          const linked = await prisma.user.update({
            where: { id: byEmail.id },
            data: { firebaseUid },
          });
          if (linked.isBlocked) {
            res.status(403).json({ error: "Account is blocked" });
            return;
          }
          const payload = buildTokenPayload(linked.id, linked.userType);
          res.json({
            accessToken: signAccessToken(payload),
            refreshToken: signRefreshToken(payload),
            user: {
              id: linked.id,
              userType: linked.userType,
              fullName: linked.fullName,
              isNewUser: false,
            },
          });
          return;
        }
      } catch (innerError: unknown) {
        console.error("[ERROR] Auth P2002 recovery failed:", innerError);
      }
    }

    console.error("[ERROR] Auth verifyOtp:", error);
    res.status(500).json({ error: "Authentication failed" });
  }
}

export async function refreshToken(req: Request, res: Response): Promise<void> {
  const { refreshToken: token } = req.body as { refreshToken?: string };
  if (!token) {
    res.status(400).json({ error: "refreshToken required" });
    return;
  }

  try {
    const payload = verifyToken(token);
    const user = await prisma.user.findUnique({ where: { id: payload.userId } });
    if (!user || user.isBlocked) {
      res.status(401).json({ error: "Invalid token" });
      return;
    }

    const newPayload = buildTokenPayload(user.id, user.userType);
    res.json({
      accessToken: signAccessToken(newPayload),
      refreshToken: signRefreshToken(newPayload),
    });
  } catch {
    res.status(401).json({ error: "Invalid or expired refresh token" });
  }
}

export async function logout(_req: Request, res: Response): Promise<void> {
  res.json({ message: "Logged out" });
}
