# AAILA Backend API

Node.js / Express serverless backend for the **AAILA** Android family communication app, built for Vercel deployment with Firebase Admin SDK and LiveKit token generation.

## Environment Variables (Vercel / `.env`)

Never commit `.env` or any secret keys. Configure these in the Vercel Dashboard under **Project Settings → Environment Variables**:

- `FIREBASE_PROJECT_ID` — Firebase project ID
- `FIREBASE_CLIENT_EMAIL` — Service account email
- `FIREBASE_PRIVATE_KEY` — Service account private key (PEM, with `\n` escaped)
- `FIREBASE_WEB_API_KEY` — Firebase Web API key (for password verification via Identity Toolkit REST API)
- `JWT_SECRET` — Random 256-bit hex secret for signing session JWTs
- `JWT_EXPIRY` — Token expiry (default `24h`)
- `LIVEKIT_API_KEY` — LiveKit Cloud API key
- `LIVEKIT_API_SECRET` — LiveKit Cloud API secret
- `LIVEKIT_URL` — LiveKit WebSocket URL (`wss://...livekit.cloud`)

## Local Development

```bash
npm install
cp .env.example .env
# Fill in .env
npm run dev
```

## Running Tests

```bash
npm test
```

## Vercel Deployment

1. Push `aaila-backend/` to a GitHub repository.
2. Import the repository in Vercel (set Root Directory to `aaila-backend` if in a monorepo).
3. Add all required Environment Variables in Vercel.
4. Deploy. Vercel will expose `/api/*` endpoints automatically.
