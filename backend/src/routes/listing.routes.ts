import { Router } from "express";
import {
  getListings,
  getListingById,
  createListing,
  updateListing,
  deleteListing,
  saveListing,
  unsaveListing,
  getSavedListings,
  revealContact,
} from "../controllers/listing.controller.js";
import { requireAuth, requireUserType } from "../middleware/auth.js";
import { UserType } from "@prisma/client";

const router = Router();

router.get("/", getListings);
router.get("/saved", requireAuth, getSavedListings);
router.get("/:id", getListingById);
router.post("/", requireAuth, requireUserType(UserType.OFFICER), createListing);
router.patch("/:id", requireAuth, requireUserType(UserType.OFFICER), updateListing);
router.delete("/:id", requireAuth, requireUserType(UserType.OFFICER), deleteListing);
router.post("/:id/save", requireAuth, saveListing);
router.delete("/:id/save", requireAuth, unsaveListing);
router.post("/:id/reveal-contact", requireAuth, revealContact);

export default router;
