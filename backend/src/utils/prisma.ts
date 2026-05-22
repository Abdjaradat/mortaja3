import { config } from "dotenv";
import path from "path";
config({ path: path.resolve(process.cwd(), ".env") });

import { PrismaClient } from "@prisma/client";
import { PrismaNeon } from "@prisma/adapter-neon";

const adapter = new PrismaNeon({ connectionString: process.env["DATABASE_URL"]! });

const prisma = new PrismaClient({
  adapter,
  log: process.env["NODE_ENV"] === "development" ? ["error", "warn"] : ["error"],
});

export default prisma;
