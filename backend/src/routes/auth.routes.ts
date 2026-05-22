import { Router } from "express";
import { verifyOtp, refreshToken, logout } from "../controllers/auth.controller.js";
import { authLimiter } from "../middleware/rateLimiter.js";

const router = Router();

// Client-side Firebase Auth handles OTP sending/verification.
// This endpoint receives the resulting Firebase ID token and issues our JWT.
router.post("/verify-firebase-token", authLimiter, verifyOtp);
router.post("/refresh", authLimiter, refreshToken);
router.post("/logout", logout);

export default router;
