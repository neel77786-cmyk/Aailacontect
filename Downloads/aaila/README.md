# AAILA - Together, Always.

AAILA is a private family communication Android application built with Jetpack Compose, Kotlin, Firebase, WebRTC 1-to-1 Audio/Video Calling, and LiveKit SFU Group Video Calling.

---

## Phase 2 Setup & Backend Configuration Guide

### 1. Firebase Console Setup Instructions

1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project named **AAILA**.
2. Register an Android App with package name `com.neel.aaila`.
3. Download the generated `google-services.json` file and place it in the `app/` directory (`app/google-services.json`).
4. **Enable Firebase Authentication**:
   - Navigate to **Build > Authentication** in Firebase Console.
   - Click **Get Started**, choose **Email/Password** sign-in method, and click **Enable**.
5. **Enable Cloud Firestore**:
   - Navigate to **Build > Firestore Database** and click **Create Database**.
   - Choose production mode and select your preferred location.
   - Go to the **Rules** tab and paste the contents of `firestore.rules`.
6. **Enable Firebase Storage**:
   - Navigate to **Build > Storage** and click **Get Started**.
   - Go to the **Rules** tab and paste the contents of `storage.rules`.
7. **Enable Cloud Messaging (FCM)**:
   - Navigate to **Project Settings > Cloud Messaging**.
   - FCM credentials are automatically bound via `google-services.json`.

---

### 2. LiveKit Group Video Calling Server Setup

1. **Self-Host or Use LiveKit Cloud**:
   - Sign up at [LiveKit Cloud](https://livekit.io/) or self-host using Docker:
     ```bash
     docker run -d --name livekit-server -p 7880:7880 -p 7881:7881 -p 50000-60000:50000-60000/udp livekit/livekit-server --dev
     ```
2. **Deploy Access Token Service**:
   - LiveKit requires signed JWT access tokens for participants joining group rooms.
   - **SECURITY RULE**: Server API Secrets (API Key & Secret) must **NEVER** be stored inside the Android client code.
   - Deploy a lightweight token generator function (Node.js/Go/Python) on Cloud Functions or your backend endpoint:
     ```javascript
     const { AccessToken } = require('livekit-server-sdk');
     const token = new AccessToken(process.env.LIVEKIT_API_KEY, process.env.LIVEKIT_API_SECRET, {
       identity: participantId,
       ttl: '1h',
     });
     token.addGrant({ roomJoin: true, room: roomName });
     return res.json({ token: await token.toJwt() });
     ```
3. Set your token server URL in `LiveKitGroupCallManager`:
   ```kotlin
   liveKitGroupCallManager.tokenServerUrl = "https://your-token-service.com/api/token"
   ```

---

### 3. Required Android Permissions
The application requests the following runtime permissions:
- `android.permission.CAMERA` (Video calling & camera switching)
- `android.permission.RECORD_AUDIO` (Voice calling & audio recording)
- `android.permission.POST_NOTIFICATIONS` (Android 13+ background call and message alerts)
- `android.permission.MODIFY_AUDIO_SETTINGS` (Speakerphone and earpiece routing)

---

### 4. Running the App
1. Open the project in Android Studio.
2. Build and run the app on an Android device or emulator (Android 7.0 / API 24+).
3. If Firebase `google-services.json` is present, live synchronization will connect automatically. If not provided, the app runs gracefully in Phase 1 UI Preview Mode.
