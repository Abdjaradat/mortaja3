import { Router } from "express";
import { requireAuth } from "../middleware/auth.js";
import {
  createRequest,
  getRequests,
  getRequestById,
  placeBid,
  closeRequest,
} from "../controllers/request.controller.js";

const router = Router();

router.post("/",           requireAuth, createRequest);
router.get("/",            requireAuth, getRequests);
router.get("/:id",         requireAuth, getRequestById);
router.post("/:id/bid",    requireAuth, placeBid);
router.post("/:id/close",  requireAuth, closeRequest);

export default router;
