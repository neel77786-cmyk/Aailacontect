import assert from "node:assert/strict";
import http from "node:http";
import jwt from "jsonwebtoken";
import { AccessToken } from "livekit-server-sdk";
import app from "../server.js";
import { signToken } from "../lib/auth-middleware.js";
import { generateLiveKitToken } from "../lib/livekit.js";
import {
  validateFamilyId,
  validatePassword,
  validateName,
} from "../lib/validators.js";

// Set test environment variables (only for local test execution)
process.env.JWT_SECRET = "test_jwt_secret_for_unit_tests_32chars_min";
process.env.JWT_EXPIRY = "1h";
process.env.LIVEKIT_API_KEY = "API_TEST_KEY_12345";
process.env.LIVEKIT_API_SECRET = "test_livekit_secret_key_that_is_long_enough_32";
process.env.LIVEKIT_URL = "wss://test-aaila.livekit.cloud";

function request(server, method, path, body = null, headers = {}) {
  return new Promise((resolve, reject) => {
    const addr = server.address();
    const options = {
      hostname: "127.0.0.1",
      port: addr.port,
      path,
      method,
      headers: {
        "Content-Type": "application/json",
        ...headers,
      },
    };

    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        let parsed = null;
        try {
          parsed = JSON.parse(data);
        } catch {
          parsed = data;
        }
        resolve({ status: res.statusCode, body: parsed });
      });
    });

    req.on("error", reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function runTests() {
  let passed = 0;
  let failed = 0;

  async function test(name, fn) {
    try {
      await fn();
      console.log(`  PASS: ${name}`);
      passed++;
    } catch (err) {
      console.error(`  FAIL: ${name}`);
      console.error(`        ${err.message}`);
      failed++;
    }
  }

  const server = await new Promise((resolve) => {
    const s = app.listen(0, "127.0.0.1", () => resolve(s));
  });

  try {
    console.log("\nRunning AAILA Backend Tests...\n");

    await test("GET /api/health returns 200 healthy", async () => {
      const res = await request(server, "GET", "/api/health");
      assert.equal(res.status, 200);
      assert.equal(res.body.success, true);
      assert.equal(res.body.status, "healthy");
    });

    await test("Validators reject invalid Family ID, password, and name", async () => {
      assert.ok(validateFamilyId("ab"));
      assert.ok(validateFamilyId("bad id!"));
      assert.equal(validateFamilyId("HASSAN-123"), null);

      assert.ok(validatePassword("123"));
      assert.equal(validatePassword("secret123"), null);

      assert.ok(validateName("A"));
      assert.equal(validateName("Neel"), null);
    });

    await test("POST /api/auth/register rejects invalid payload with 400", async () => {
      const res = await request(server, "POST", "/api/auth/register", {
        familyName: "A",
        adminName: "B",
        familyId: "X",
        password: "123",
      });
      assert.equal(res.status, 400);
      assert.equal(res.body.success, false);
      assert.ok(Array.isArray(res.body.errors));
      assert.equal(res.body.errors.length, 4);
    });

    await test("POST /api/auth/login rejects invalid payload with 400", async () => {
      const res = await request(server, "POST", "/api/auth/login", {
        familyId: "x",
        password: "12",
      });
      assert.equal(res.status, 400);
      assert.equal(res.body.success, false);
    });

    await test("Protected routes return 401 when Authorization header is missing", async () => {
      const meRes = await request(server, "GET", "/api/auth/me");
      assert.equal(meRes.status, 401);

      const membersRes = await request(server, "GET", "/api/family/members");
      assert.equal(membersRes.status, 401);

      const lkRes = await request(server, "POST", "/api/livekit/token", {
        roomName: "family-room",
      });
      assert.equal(lkRes.status, 401);
    });

    await test("Protected routes return 401 for malformed or invalid JWT", async () => {
      const res = await request(
        server,
        "GET",
        "/api/auth/me",
        null,
        { Authorization: "Bearer invalid.jwt.token" }
      );
      assert.equal(res.status, 401);
      assert.equal(res.body.success, false);
    });

    await test("signToken creates valid JWT with uid, familyId, role", async () => {
      const token = signToken("uid_12345", "HASSAN-123", "ADMIN");
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      assert.equal(decoded.uid, "uid_12345");
      assert.equal(decoded.familyId, "HASSAN-123");
      assert.equal(decoded.role, "ADMIN");
    });

    await test("POST /api/auth/logout succeeds with valid Bearer token", async () => {
      const token = signToken("uid_12345", "HASSAN-123", "ADMIN");
      const res = await request(
        server,
        "POST",
        "/api/auth/logout",
        {},
        { Authorization: `Bearer ${token}` }
      );
      assert.equal(res.status, 200);
      assert.equal(res.body.success, true);
    });

    await test("POST /api/livekit/token rejects missing roomName with 400", async () => {
      const token = signToken("uid_12345", "HASSAN-123", "ADMIN");
      const res = await request(
        server,
        "POST",
        "/api/livekit/token",
        {},
        { Authorization: `Bearer ${token}` }
      );
      assert.equal(res.status, 400);
      assert.equal(res.body.success, false);
    });

    await test("POST /api/livekit/token generates valid LiveKit JWT for authenticated user", async () => {
      const authToken = signToken("uid_12345", "HASSAN-123", "ADMIN");
      const res = await request(
        server,
        "POST",
        "/api/livekit/token",
        { roomName: "SundayCall", participantName: "Neel" },
        { Authorization: `Bearer ${authToken}` }
      );
      assert.equal(res.status, 200);
      assert.equal(res.body.success, true);
      assert.equal(res.body.url, "wss://test-aaila.livekit.cloud");
      assert.equal(res.body.room, "HASSAN-123_SundayCall");
      assert.ok(res.body.token);

      // Verify the LiveKit JWT is signed with LIVEKIT_API_SECRET and contains video grant
      const decodedLk = jwt.verify(res.body.token, process.env.LIVEKIT_API_SECRET);
      assert.equal(decodedLk.sub, "HASSAN-123_uid_12345");
      assert.equal(decodedLk.name, "Neel");
      assert.equal(decodedLk.video.room, "HASSAN-123_SundayCall");
      assert.equal(decodedLk.video.roomJoin, true);
    });

    await test("generateLiveKitToken helper generates valid JWT", async () => {
      const res = await generateLiveKitToken("user_abc", "Asha", "FamilyRoom");
      assert.ok(res.token);
      assert.equal(res.url, "wss://test-aaila.livekit.cloud");
      const decoded = jwt.verify(res.token, process.env.LIVEKIT_API_SECRET);
      assert.equal(decoded.sub, "user_abc");
      assert.equal(decoded.video.room, "FamilyRoom");
    });

    console.log(`\nResults: ${passed} passed, ${failed} failed.\n`);
    if (failed > 0) process.exitCode = 1;
  } finally {
    server.close();
  }
}

runTests();
