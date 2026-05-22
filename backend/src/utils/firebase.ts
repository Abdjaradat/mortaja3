import admin from "firebase-admin";

function initializeFirebase(): boolean {
  if (admin.apps.length) return true;

  const privateKey = process.env["FIREBASE_PRIVATE_KEY"]?.replace(/\\n/g, "\n");
  if (!process.env["FIREBASE_PROJECT_ID"] || !privateKey || !process.env["FIREBASE_CLIENT_EMAIL"]) {
    console.warn("[Firebase] Admin SDK not initialized — credentials missing in .env");
    return false;
  }

  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: process.env["FIREBASE_PROJECT_ID"],
      privateKey,
      clientEmail: process.env["FIREBASE_CLIENT_EMAIL"],
    }),
  });
  return true;
}

export async function verifyFirebaseIdToken(idToken: string): Promise<admin.auth.DecodedIdToken> {
  if (!initializeFirebase()) {
    throw new Error("Firebase Admin SDK is not configured");
  }
  return admin.auth().verifyIdToken(idToken);
}

export async function sendPushNotification(
  fcmToken: string,
  title: string,
  body: string,
  data?: Record<string, string>,
): Promise<void> {
  if (!initializeFirebase()) return;
  try {
    await admin.messaging().send({
      token: fcmToken,
      notification: { title, body },
      ...(data ? { data } : {}),
      android: { priority: "high" },
    });
  } catch (error) {
    console.error("[FCM] send failed:", error);
  }
}

export default admin;
