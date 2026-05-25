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
  console.log("[AUTH] Starting token verification");

  const parsed = verifyFirebaseTokenSchema.safeParse(req.body);
  if (!parsed.success) {
    console.log("[AUTH] Schema validation failed:", JSON.stringify(parsed.error.flatten()));
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const { idToken, userType } = parsed.data;
  console.log("[AUTH] Schema OK. userType:", userType ?? "not provided");

  let decoded;
  try {
    console.log("[AUTH] Verifying Firebase token...");
    decoded = await verifyFirebaseIdToken(idToken);
    console.log("[AUTH] Token verified. UID:", decoded.uid, "Email:", decoded.email ?? "none");
  } catch (firebaseError: unknown) {
    const msg = firebaseError instanceof Error ? firebaseError.message : String(firebaseError);
    console.error("[AUTH] Firebase verification failed:", msg);
    res.status(401).json({ error: "Invalid Firebase ID token" });
    return;
  }

  const firebaseUid = decoded.uid;
  const email = decoded.email ?? null;

  try {
    console.log("[AUTH] Looking up user by firebaseUid:", firebaseUid);
    let user = await prisma.user.findUnique({ where: { firebaseUid } });
    console.log("[AUTH] User by firebaseUid:", user ? `found (id=${user.id})` : "not found");
    let isNewUser = !user;

    if (!user) {
      if (email) {
        console.log("[AUTH] Looking up user by email:", email);
        const byEmail = await prisma.user.findUnique({ where: { email } });
        console.log("[AUTH] User by email:", byEmail ? `found (id=${byEmail.id})` : "not found");
        if (byEmail) {
          console.log("[AUTH] Linking existing user to new Firebase UID...");
          user = await prisma.user.update({
            where: { id: byEmail.id },
            data: { firebaseUid },
          });
          console.log("[AUTH] User linked successfully");
          isNewUser = false;
        }
      }

      if (!user) {
        if (!userType) {
          console.log("[AUTH] New user, no userType provided — returning 400");
          res.status(400).json({ error: "userType required for first registration" });
          return;
        }
        const referralCode = generateReferralCode();
        console.log("[AUTH] Creating new user. userType:", userType, "referralCode:", referralCode);
        user = await prisma.user.create({
          data: { firebaseUid, email, userType, referralCode },
        });
        console.log("[AUTH] User created:", user.id);

        console.log("[AUTH] Creating welcome token transaction...");
        await prisma.tokenTransaction.create({
          data: {
            userId: user.id,
            type: TxType.EARN,
            amount: 500,
            reason: TxReason.WELCOME,
            balanceAfter: 500,
          },
        });
        console.log("[AUTH] Welcome transaction created");
      }
    } else if (email && user.email !== email) {
      console.log("[AUTH] Updating email for existing user...");
      user = await prisma.user.update({
        where: { firebaseUid },
        data: { email },
      });
      console.log("[AUTH] Email updated");
    }

    if (user.isBlocked) {
      console.log("[AUTH] User is blocked:", user.id);
      res.status(403).json({ error: "Account is blocked" });
      return;
    }

    console.log("[AUTH] Generating JWT for user:", user.id, "userType:", user.userType);
    const payload = buildTokenPayload(user.id, user.userType);
    console.log("[AUTH] Success — returning tokens");

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
    const code = (error as Record<string, unknown>)["code"];
    const msg  = error instanceof Error ? error.message : String(error);
    console.error("[AUTH] Caught error. code:", code, "message:", msg);

    if (code === "P2002" && email) {
      console.log("[AUTH] P2002 — attempting recovery by email:", email);
      try {
        const byEmail = await prisma.user.findUnique({ where: { email } });
        if (byEmail) {
          console.log("[AUTH] P2002 recovery: found user by email, linking UID...");
          const linked = await prisma.user.update({
            where: { id: byEmail.id },
            data: { firebaseUid },
          });
          if (linked.isBlocked) {
            res.status(403).json({ error: "Account is blocked" });
            return;
          }
          const payload = buildTokenPayload(linked.id, linked.userType);
          console.log("[AUTH] P2002 recovery succeeded for user:", linked.id);
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
        console.error("[AUTH] P2002 recovery: no user found by email");
      } catch (innerError: unknown) {
        console.error("[AUTH] P2002 recovery failed:", innerError);
      }
    }

    console.error("[AUTH] Unhandled error — returning 500:", error);
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
