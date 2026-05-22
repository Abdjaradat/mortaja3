import jwt from "jsonwebtoken";
import type { JwtPayload } from "../types/index.js";
import { UserType } from "@prisma/client";

const JWT_SECRET = process.env["JWT_SECRET"] ?? "";
const JWT_EXPIRES_IN = process.env["JWT_EXPIRES_IN"] ?? "7d";
const JWT_REFRESH_EXPIRES_IN = process.env["JWT_REFRESH_EXPIRES_IN"] ?? "30d";

if (!JWT_SECRET) {
  throw new Error("JWT_SECRET env variable is not set");
}

export function signAccessToken(payload: JwtPayload): string {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN } as jwt.SignOptions);
}

export function signRefreshToken(payload: JwtPayload): string {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_REFRESH_EXPIRES_IN } as jwt.SignOptions);
}

export function verifyToken(token: string): JwtPayload {
  const decoded = jwt.verify(token, JWT_SECRET) as JwtPayload;
  return decoded;
}

export function buildTokenPayload(userId: string, userType: UserType): JwtPayload {
  return { userId, userType };
}
