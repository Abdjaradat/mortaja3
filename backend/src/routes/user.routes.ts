import { Router } from "express";
import {
  getMe, updateMe, updateFcmToken, deleteMe,
  submitOfficerProfile, getOfficerProfileStatus,
} from "../controllers/user.controller.js";
import { requireAuth, requireUserType } from "../middleware/auth.js";
import { UserType } from "@prisma/client";

const router = Router();

router.use(requireAuth);

router.get("/me", getMe);
router.patch("/me", updateMe);
router.patch("/me/fcm-token", updateFcmToken);
router.delete("/me", deleteMe);
router.post("/me/officer-profile", requireUserType(UserType.OFFICER), submitOfficerProfile);
router.get("/me/officer-profile/status", requireUserType(UserType.OFFICER), getOfficerProfileStatus);

export default router;
