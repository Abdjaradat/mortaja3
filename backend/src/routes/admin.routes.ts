import { Router } from "express";
import { getPendingVerifications, approveVerification, rejectVerification, getReports, blockUser } from "../controllers/admin.controller.js";
import { requireAuth, requireUserType } from "../middleware/auth.js";
import { UserType } from "@prisma/client";

const router = Router();

router.use(requireAuth, requireUserType(UserType.ADMIN));

router.get("/pending-verifications", getPendingVerifications);
router.post("/verifications/:id/approve", approveVerification);
router.post("/verifications/:id/reject", rejectVerification);
router.get("/reports", getReports);
router.post("/users/:id/block", blockUser);

export default router;
