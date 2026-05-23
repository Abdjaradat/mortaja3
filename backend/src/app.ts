import express from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";
import prisma from "./utils/prisma.js";

import { globalLimiter } from "./middleware/rateLimiter.js";
import authRoutes from "./routes/auth.routes.js";
import userRoutes from "./routes/user.routes.js";
import tokenRoutes from "./routes/token.routes.js";
import listingRoutes from "./routes/listing.routes.js";
import chatRoutes from "./routes/chat.routes.js";
import adminRoutes from "./routes/admin.routes.js";
import reportRoutes from "./routes/report.routes.js";
import requestRoutes from "./routes/request.routes.js";
import clearanceRoutes from "./routes/clearance.routes.js";

const app = express();

app.use(
  helmet({
    hsts: { maxAge: 31536000, includeSubDomains: true },
  })
);

app.use(
  cors({
    origin: process.env["CORS_ORIGIN"] ?? "http://localhost:3000",
    credentials: true,
  })
);

app.use(express.json({ limit: "1mb" }));
app.use(morgan("combined"));
app.use(globalLimiter);

app.use("/api/v1/auth", authRoutes);
app.use("/api/v1/users", userRoutes);
app.use("/api/v1/tokens", tokenRoutes);
app.use("/api/v1/listings", listingRoutes);
app.use("/api/v1/conversations", chatRoutes);
app.use("/api/v1/admin", adminRoutes);
app.use("/api/v1/reports", reportRoutes);
app.use("/api/v1/requests", requestRoutes);
app.use("/api/v1/clearance", clearanceRoutes);

app.get("/health", async (_req, res) => {
  let dbStatus = "untested";
  let dbError: string | null = null;
  try {
    await prisma.$queryRaw`SELECT 1`;
    dbStatus = "connected";
  } catch (e: unknown) {
    dbStatus = "error";
    dbError = e instanceof Error ? e.message : String(e);
  }
  res.json({
    status: "ok",
    db: process.env["DATABASE_URL"] ? dbStatus : "missing",
    dbError,
    firebase: process.env["FIREBASE_PROJECT_ID"] ? "set" : "missing",
    node_env: process.env["NODE_ENV"],
  });
});

app.use((_req, res) => {
  res.status(404).json({ error: "Not found" });
});

export default app;
