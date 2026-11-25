/**
 * Verifies that all 9 required environment variables are configured and well-formed.
 * NEVER prints secret values to stdout or logs.
 */

const REQUIRED_VARS = [
  {
    name: "FIREBASE_PROJECT_ID",
    validate: (v) => v.length > 0 && !v.includes("your-firebase-project-id"),
    hint: "Must be your real Firebase Project ID",
  },
  {
    name: "FIREBASE_CLIENT_EMAIL",
    validate: (v) => v.includes("@") && v.includes(".iam.gserviceaccount.com"),
    hint: "Must be a Firebase service account email (*.iam.gserviceaccount.com)",
  },
  {
    name: "FIREBASE_PRIVATE_KEY",
    validate: (v) =>
      v.includes("BEGIN PRIVATE KEY") && v.includes("END PRIVATE KEY"),
    hint: "Must be a PEM private key containing BEGIN/END PRIVATE KEY",
  },
  {
    name: "FIREBASE_WEB_API_KEY",
    validate: (v) => v.length > 10 && !v.includes("your-firebase-web-api-key"),
    hint: "Must be your Firebase Web API Key",
  },
  {
    name: "JWT_SECRET",
    validate: (v) => v.length >= 32 && !v.includes("your-random-256-bit"),
    hint: "Must be at least 32 characters (64+ hex chars recommended)",
  },
  {
    name: "JWT_EXPIRY",
    validate: (v) => /^\d+[smhd]$/.test(v),
    hint: "Must be a duration like 24h, 7d, or 3600s",
  },
  {
    name: "LIVEKIT_API_KEY",
    validate: (v) => v.length > 5 && !v.includes("APIxxxxxxxx"),
    hint: "Must be your LiveKit Cloud API Key",
  },
  {
    name: "LIVEKIT_API_SECRET",
    validate: (v) => v.length >= 16 && !v.includes("your-livekit-api-secret"),
    hint: "Must be your LiveKit Cloud API Secret",
  },
  {
    name: "LIVEKIT_URL",
    validate: (v) =>
      (v.startsWith("wss://") || v.startsWith("ws://")) &&
      !v.includes("your-app.livekit.cloud"),
    hint: "Must be a WebSocket URL starting with wss://",
  },
];

let missingOrInvalid = 0;

console.log("\nAAILA Backend Environment Variable Verification:\n");

for (const item of REQUIRED_VARS) {
  const raw = process.env[item.name];
  if (!raw || raw.trim().length === 0) {
    console.error(`  [MISSING] ${item.name} — not set`);
    missingOrInvalid++;
  } else if (!item.validate(raw.trim())) {
    console.error(`  [INVALID] ${item.name} — ${item.hint}`);
    missingOrInvalid++;
  } else {
    console.log(`  [OK]      ${item.name} (set, length=${raw.length})`);
  }
}

console.log("");

if (missingOrInvalid > 0) {
  console.error(
    `Verification failed: ${missingOrInvalid} variable(s) missing or invalid.\n`
  );
  process.exit(1);
} else {
  console.log("All 9 environment variables verified successfully.\n");
}
