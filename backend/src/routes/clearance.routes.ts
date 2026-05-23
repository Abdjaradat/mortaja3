import { Router } from "express";
import { requireAuth } from "../middleware/auth.js";
import {
  registerAgent,
  listAgents,
  getMyAgent,
  getAgentById,
  createRequest,
  listRequests,
  getRequestById,
  submitOffer,
  selectAgent,
  rateAgent,
} from "../controllers/clearance.controller.js";

const router = Router();

// Agents
router.post("/agents",     requireAuth, registerAgent);
router.get("/agents",      requireAuth, listAgents);
router.get("/agents/me",   requireAuth, getMyAgent);
router.get("/agents/:id",  requireAuth, getAgentById);

// Requests
router.post("/requests",             requireAuth, createRequest);
router.get("/requests",              requireAuth, listRequests);
router.get("/requests/:id",          requireAuth, getRequestById);
router.post("/requests/:id/offer",   requireAuth, submitOffer);
router.post("/requests/:id/select",  requireAuth, selectAgent);
router.post("/requests/:id/rate",    requireAuth, rateAgent);

export default router;
