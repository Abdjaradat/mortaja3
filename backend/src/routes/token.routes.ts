import { Router } from "express";
import { getTokenBalance, watchAd, spendTokens } from "../controllers/token.controller.js";
import { requireAuth } from "../middleware/auth.js";

const router = Router();
router.use(requireAuth);

router.get("/balance", getTokenBalance);
router.post("/watch-ad", watchAd);
router.post("/spend", spendTokens);

export default router;
