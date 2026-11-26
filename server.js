import express from "express";
import cors from "cors";
import healthHandler from "./api/health.js";
import registerHandler from "./api/auth/register.js";
import loginHandler from "./api/auth/login.js";
import meHandler from "./api/auth/me.js";
import logoutHandler from "./api/auth/logout.js";
import passwordHandler from "./api/auth/password.js";
import membersHandler from "./api/family/members.js";
import memberByUidHandler from "./api/family/members/[uid].js";
import invitesHandler from "./api/family/invites.js";
import joinHandler from "./api/family/join.js";
import profileHandler from "./api/family/profile.js";
import threadsHandler from "./api/chats/threads.js";
import messagesHandler from "./api/chats/messages.js";
import historyHandler from "./api/calls/history.js";
import initiateHandler from "./api/calls/initiate.js";
import callByIdHandler from "./api/calls/[callId].js";
import livekitTokenHandler from "./api/livekit/token.js";

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Route mapping matching Vercel serverless functions
app.all("/api/health", (req, res) => healthHandler(req, res));
app.all("/api/auth/register", (req, res) => registerHandler(req, res));
app.all("/api/auth/login", (req, res) => loginHandler(req, res));
app.all("/api/auth/me", (req, res) => meHandler(req, res));
app.all("/api/auth/logout", (req, res) => logoutHandler(req, res));
app.all("/api/auth/password", (req, res) => passwordHandler(req, res));
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
