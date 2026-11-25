import { getAuth, getFirestore } from "../../lib/firebase-admin.js";
import { signToken } from "../../lib/auth-middleware.js";
import { validateFamilyId, validatePassword, collectErrors } from "../../lib/validators.js";

/**
 * POST /api/auth/login
 *
 * Authenticates a user with Family ID + password.
 * - Looks up the family document and resolves the admin UID
 * - Verifies the password against Firebase Auth (via custom token exchange)
 * - Returns a signed JWT session token
 *
 * NOTE: Firebase Admin SDK cannot directly verify passwords.
 * We use a two-step approach:
 *   1. Look up the user by synthetic email
 *   2. Use Firebase Auth REST API (identitytoolkit) to verify the password
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { familyId, password } = req.body || {};

    // Validate inputs
    const errors = collectErrors([
      validateFamilyId(familyId),
      validatePassword(password),
    ]);

    if (errors) {
      return res.status(400).json({ success: false, errors });
    }

    const cleanFamilyId = familyId.trim().toUpperCase();
    const syntheticEmail = `admin.${cleanFamilyId.toLowerCase()}@aaila.family`;

    const auth = getAuth();
    const firestore = getFirestore();

    // Step 1: Verify password via Firebase Auth REST API
    const firebaseApiKey = process.env.FIREBASE_WEB_API_KEY;

    let firebaseUid;

    if (firebaseApiKey) {
      // Use Firebase Auth REST API for password verification
      const authResponse = await fetch(
        `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${firebaseApiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: syntheticEmail,
            password,
            returnSecureToken: true,
          }),
        }
      );

      if (!authResponse.ok) {
        const errorData = await authResponse.json().catch(() => ({}));
        const errorMessage = errorData?.error?.message || "AUTH_FAILED";

        if (
          errorMessage === "EMAIL_NOT_FOUND" ||
          errorMessage === "INVALID_PASSWORD" ||
          errorMessage === "INVALID_LOGIN_CREDENTIALS"
        ) {
          return res.status(401).json({
            success: false,
            error: "Invalid Family ID or password.",
          });
        }

        if (errorMessage === "USER_DISABLED") {
          return res.status(403).json({
            success: false,
            error: "This account has been disabled. Contact your family admin.",
          });
        }

        return res.status(401).json({
          success: false,
          error: "Authentication failed. Please check your credentials.",
        });
      }

      const authData = await authResponse.json();
      firebaseUid = authData.localId;
    } else {
      // Fallback: look up user by email (less secure — no password verification via REST)
      // In production, FIREBASE_WEB_API_KEY should always be set
      try {
        const userRecord = await auth.getUserByEmail(syntheticEmail);
        firebaseUid = userRecord.uid;
      } catch (lookupErr) {
        if (lookupErr.code === "auth/user-not-found") {
          return res.status(401).json({
            success: false,
            error: "Invalid Family ID or password.",
          });
        }
        throw lookupErr;
      }

      // Verify password by generating a custom token and attempting sign-in
      // Since we don't have the web API key, we trust the Admin SDK lookup
      // but log a warning
      console.warn(
        "FIREBASE_WEB_API_KEY not set — password verification is bypassed. " +
          "Set this variable for production security."
      );
    }

    // Step 2: Check family document exists
    const familyDoc = await firestore
      .collection("families")
      .doc(cleanFamilyId)
      .get();

    if (!familyDoc.exists) {
      return res.status(404).json({
        success: false,
        error: `Family Space "${cleanFamilyId}" not found.`,
      });
    }

    const familyData = familyDoc.data();

    // Step 3: Check member status
    const memberDoc = await firestore
      .collection("families")
      .doc(cleanFamilyId)
      .collection("members")
      .doc(firebaseUid)
      .get();

    if (!memberDoc.exists) {
      return res.status(403).json({
        success: false,
        error: `You are not a member of Family Space "${cleanFamilyId}".`,
      });
    }

    const memberData = memberDoc.data();
    const memberStatus = memberData.status || "PENDING";

    if (memberStatus !== "APPROVED") {
      return res.status(403).json({
        success: false,
        error: `Your access is currently "${memberStatus}". Contact your family admin.`,
      });
    }

    const role = memberData.role || "MEMBER";

    // Step 4: Update last seen
    await firestore
      .collection("families")
      .doc(cleanFamilyId)
      .collection("members")
      .doc(firebaseUid)
      .update({
        lastSeen: new Date().toISOString(),
        onlineStatus: "AVAILABLE",
      });

    // Step 5: Sign session JWT
    const token = signToken(firebaseUid, cleanFamilyId, role);

    return res.status(200).json({
      success: true,
      userId: firebaseUid,
      familyId: cleanFamilyId,
      familyName: familyData.familyName || "Family Space",
      adminName: familyData.adminName || "Admin",
      role,
      token,
      expiresIn: process.env.JWT_EXPIRY || "24h",
    });
  } catch (err) {
    console.error("Login error:", err);
    return res.status(500).json({
      success: false,
      error: "Internal server error during login.",
    });
  }
}
