FROM node:20-alpine AS builder
# Cache bust: 2026-05-25
WORKDIR /app
COPY backend/package*.json ./
COPY backend/prisma ./prisma/
RUN npm ci --ignore-scripts && npx prisma generate
COPY backend/tsconfig*.json ./
COPY backend/src ./src
COPY backend/prisma.config.ts ./
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package*.json ./
COPY --from=builder /app/prisma ./prisma

EXPOSE 3000
CMD ["node", "dist/server.js"]
