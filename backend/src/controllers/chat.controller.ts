import { Response } from "express";
import { z } from "zod";
import prisma from "../utils/prisma.js";
import type { AuthenticatedRequest } from "../middleware/auth.js";
import { sendPushNotification } from "../utils/firebase.js";

const sendMessageSchema = z.object({
  content: z.string().min(1).max(2000),
});

const startConversationSchema = z.object({
  otherUserId: z.string().min(1),
  listingId: z.string().optional(),
});

export async function getConversations(req: AuthenticatedRequest, res: Response): Promise<void> {
  const userId = req.user!.userId;

  const conversations = await prisma.conversation.findMany({
    where: { OR: [{ user1Id: userId }, { user2Id: userId }] },
    include: {
      user1: { select: { id: true, fullName: true, photoUrl: true } },
      user2: { select: { id: true, fullName: true, photoUrl: true } },
      messages: {
        orderBy: { createdAt: "desc" },
        take: 1,
        select: { content: true, createdAt: true, isRead: true, senderId: true },
      },
    },
    orderBy: { updatedAt: "desc" },
  });

  res.json(conversations);
}

export async function getMessages(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id: conversationId } = req.params as { id: string };
  const userId = req.user!.userId;

  const conversation = await prisma.conversation.findFirst({
    where: { id: conversationId, OR: [{ user1Id: userId }, { user2Id: userId }] },
  });

  if (!conversation) {
    res.status(404).json({ error: "Conversation not found" });
    return;
  }

  const messages = await prisma.message.findMany({
    where: { conversationId },
    orderBy: { createdAt: "asc" },
    select: { id: true, content: true, senderId: true, isRead: true, createdAt: true },
  });

  await prisma.message.updateMany({
    where: { conversationId, receiverId: userId, isRead: false },
    data: { isRead: true },
  });

  res.json(messages);
}

export async function sendMessage(req: AuthenticatedRequest, res: Response): Promise<void> {
  const { id: conversationId } = req.params as { id: string };
  const userId = req.user!.userId;

  const conversation = await prisma.conversation.findFirst({
    where: { id: conversationId, OR: [{ user1Id: userId }, { user2Id: userId }] },
  });

  if (!conversation) {
    res.status(404).json({ error: "Conversation not found" });
    return;
  }

  const parsed = sendMessageSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const receiverId = conversation.user1Id === userId ? conversation.user2Id : conversation.user1Id;

  const message = await prisma.message.create({
    data: {
      conversationId,
      senderId: userId,
      receiverId,
      content: parsed.data.content,
    },
    select: { id: true, content: true, senderId: true, createdAt: true },
  });

  await prisma.conversation.update({
    where: { id: conversationId },
    data: { updatedAt: new Date() },
  });

  const [receiver, sender] = await Promise.all([
    prisma.user.findUnique({ where: { id: receiverId }, select: { fcmToken: true } }),
    prisma.user.findUnique({ where: { id: userId }, select: { fullName: true } }),
  ]);

  if (receiver?.fcmToken) {
    await sendPushNotification(
      receiver.fcmToken,
      sender?.fullName ?? "رائد",
      parsed.data.content,
      { type: "NEW_MESSAGE", conversationId, messageId: message.id },
    );
  }

  res.status(201).json(message);
}

export async function startConversation(req: AuthenticatedRequest, res: Response): Promise<void> {
  const parsed = startConversationSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const userId = req.user!.userId;
  const { otherUserId, listingId } = parsed.data;

  if (otherUserId === userId) {
    res.status(400).json({ error: "Cannot start conversation with yourself" });
    return;
  }

  const otherUser = await prisma.user.findUnique({ where: { id: otherUserId } });
  if (!otherUser) {
    res.status(404).json({ error: "User not found" });
    return;
  }

  // Sort IDs for consistent user1/user2 ordering
  const [user1Id, user2Id] = [userId, otherUserId].sort();

  const include = {
    user1: { select: { id: true, fullName: true, photoUrl: true } },
    user2: { select: { id: true, fullName: true, photoUrl: true } },
    messages: { orderBy: { createdAt: "desc" as const }, take: 1, select: { content: true, createdAt: true, isRead: true, senderId: true } },
  };

  const existing = await prisma.conversation.findFirst({
    where: { user1Id, user2Id, listingId: listingId ?? null },
    include,
  });

  if (existing) {
    res.json(existing);
    return;
  }

  const conversation = await prisma.conversation.create({
    data: { user1Id, user2Id, listingId: listingId ?? null },
    include,
  });

  res.status(201).json(conversation);
}
