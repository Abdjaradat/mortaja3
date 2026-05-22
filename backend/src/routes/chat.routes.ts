import { Router } from "express";
import { getConversations, getMessages, sendMessage, startConversation } from "../controllers/chat.controller.js";
import { requireAuth } from "../middleware/auth.js";

const router = Router();

router.use(requireAuth);

router.get("/", getConversations);
router.post("/", startConversation);
router.get("/:id/messages", getMessages);
router.post("/:id/messages", sendMessage);

export default router;
