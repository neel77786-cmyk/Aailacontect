import admin from "firebase-admin";
import { getFirestore as getFirestoreDb } from "firebase-admin/firestore";

const DEFAULT_DATABASE_ID = "ai-studio-android-aaila-8beedba0-911b-40e5-9d52-12347989dd34";

let appInstance = null;

export function getFirebaseAdmin() {
  if (!appInstance) {
    const projectId = process.env.FIREBASE_PROJECT_ID?.trim();
    const clientEmail = process.env.FIREBASE_CLIENT_EMAIL?.trim();
    const rawKey = process.env.FIREBASE_PRIVATE_KEY?.trim();
    const privateKey = rawKey
      ?.replace(/^["']|["']$/g, "")
      ?.replace(/\\n/g, "\n");

    if (!projectId || !clientEmail || !privateKey) {
      throw new Error(
        "Missing Firebase Admin SDK environment variables. " +
          "Set FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, and FIREBASE_PRIVATE_KEY."
      );
    }

    appInstance = admin.apps.length
      ? admin.app()
      : admin.initializeApp({
          credential: admin.credential.cert({
            projectId,
            clientEmail,
            privateKey,
          }),
        });
  }

  return admin;
}

export function getAuth() {
  getFirebaseAdmin();
  return admin.auth();
}

export function getFirestore() {
  getFirebaseAdmin();
  const databaseId = process.env.FIREBASE_DATABASE_ID?.trim() || DEFAULT_DATABASE_ID;
  return getFirestoreDb(appInstance, databaseId);
}
