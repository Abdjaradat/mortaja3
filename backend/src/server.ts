import "dotenv/config";
import http from "http";
import { Server as SocketServer } from "socket.io";
import app from "./app.js";
import prisma from "./utils/prisma.js";

const PORT = parseInt(process.env["PORT"] ?? "3000", 10);

const httpServer = http.createServer(app);

// Socket.io setup — real-time chat wired in Sprint 5
const io = new SocketServer(httpServer, {
  cors: {
    origin: process.env["CORS_ORIGIN"] ?? "http://localhost:3000",
    credentials: true,
  },
});

io.on("connection", (socket) => {
  // TODO (Sprint 5): authenticate socket, join user room, handle message events
  console.log(`Socket connected: ${socket.id}`);

  socket.on("disconnect", () => {
    console.log(`Socket disconnected: ${socket.id}`);
  });
});

async function main(): Promise<void> {
  await prisma.$connect();
  console.log("Database connected");

  httpServer.listen(PORT, () => {
    console.log(`Ra'ed backend running on port ${PORT}`);
  });
}

main().catch((err) => {
  console.error("Failed to start server:", err);
  process.exit(1);
});
