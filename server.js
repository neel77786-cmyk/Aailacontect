import express from "express";
import cors from "cors";
import healthHandler from "./handlers/health.js";
import registerHandler from "./handlers/auth/register.js";
import loginHandler from "./handlers/auth/login.js";
import meHandler from "./handlers/auth/me.js";
import logoutHandler from "./handlers/auth/logout.js";
import refreshHandler from "./handlers/auth/refresh.js";
import passwordHandler from "./handlers/auth/password.js";
import resetPasswordHandler from "./handlers/auth/reset-password.js";
import switchFamilyHandler from "./handlers/auth/switch-family.js";
import membersHandler from "./handlers/family/members.js";
import memberByUidHandler from "./handlers/family/members/[uid].js";
import invitesHandler from "./handlers/family/invites.js";
import joinHandler from "./handlers/family/join.js";
import profileHandler from "./handlers/family/profile.js";
import threadsHandler from "./handlers/chats/threads.js";
import messagesHandler from "./handlers/chats/messages.js";
import historyHandler from "./handlers/calls/history.js";
import initiateHandler from "./handlers/calls/initiate.js";
import callByIdHandler from "./handlers/calls/[callId].js";
import livekitTokenHandler from "./handlers/livekit/token.js";

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Route mapping for all AAILA endpoints
app.all("/api/health", (req, res) => healthHandler(req, res));
app.all("/api/auth/register", (req, res) => registerHandler(req, res));
app.all("/api/auth/login", (req, res) => loginHandler(req, res));
app.all("/api/auth/me", (req, res) => meHandler(req, res));
app.all("/api/auth/logout", (req, res) => logoutHandler(req, res));
app.all("/api/auth/refresh", (req, res) => refreshHandler(req, res));
app.all("/api/auth/password", (req, res) => passwordHandler(req, res));
app.all("/api/auth/reset-password", (req, res) => resetPasswordHandler(req, res));
app.all("/api/auth/switch-family", (req, res) => switchFamilyHandler(req, res));
app.all("/api/family/members", (req, res) => membersHandler(req, res));
app.all("/api/family/members/:uid", (req, res) => {
  req.query = { ...req.query, uid: req.params.uid };
  return memberByUidHandler(req, res);
});
app.all("/api/family/invites", (req, res) => invitesHandler(req, res));
app.all("/api/family/join", (req, res) => joinHandler(req, res));
app.all("/api/family/profile", (req, res) => profileHandler(req, res));
app.all("/api/chats/threads", (req, res) => threadsHandler(req, res));
app.all("/api/chats/messages", (req, res) => messagesHandler(req, res));
app.all("/api/calls/history", (req, res) => historyHandler(req, res));
app.all("/api/calls/initiate", (req, res) => initiateHandler(req, res));
app.all("/api/calls/:callId", (req, res) => {
  req.query = { ...req.query, callId: req.params.callId };
  return callByIdHandler(req, res);
});
app.all("/api/livekit/token", (req, res) => livekitTokenHandler(req, res));

// 404 fallback
app.use((req, res) => {
  res.status(404).json({ success: false, error: "Endpoint not found." });
});

// Start server only when run directly
if (process.argv[1] && process.argv[1].endsWith("server.js")) {
  app.listen(PORT, () => {
    console.log(`AAILA Backend listening on http://localhost:${PORT}`);
  });
}

export default app;
