import { Request, Response, NextFunction } from "express";
import { verifyToken } from "../utils/jwt.js";
import { UserType } from "@prisma/client";

export interface AuthenticatedRequest extends Request {
  user?: { userId: string; userType: UserType };
}

export function requireAuth(req: AuthenticatedRequest, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith("Bearer ")) {
    res.status(401).json({ error: "Unauthorized" });
    return;
  }

  const token = authHeader.slice(7);
  try {
    const payload = verifyToken(token);
    req.user = { userId: payload.userId, userType: payload.userType };
    next();
  } catch {
    res.status(401).json({ error: "Invalid or expired token" });
  }
}

export function requireUserType(...types: UserType[]) {
  return (req: AuthenticatedRequest, res: Response, next: NextFunction): void => {
    if (!req.user || !types.includes(req.user.userType)) {
      res.status(403).json({ error: "Forbidden" });
      return;
    }
    next();
  };
}
