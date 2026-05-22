import { UserType } from "@prisma/client";

export interface JwtPayload {
  userId: string;
  userType: UserType;
  iat?: number;
  exp?: number;
}

export interface AuthRequest extends Express.Request {
  user?: JwtPayload;
}
