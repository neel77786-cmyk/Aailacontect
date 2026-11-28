import crypto from "node:crypto";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import nodemailer from "nodemailer";
import { getFirestore } from "./firebase-admin.js";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const OTP_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
const MAX_OTP_ATTEMPTS = 5;

export function normalizeEmail(raw) {
  if (typeof raw !== "string") return "";
  return raw.trim().toLowerCase();
}

export function isValidEmail(raw) {
  const email = normalizeEmail(raw);
  return email.length >= 5 && email.length <= 160 && EMAIL_REGEX.test(email);
}

function createTransporter() {
  const gmailUser = process.env.GMAIL_USER?.trim();
  const gmailPass = process.env.GMAIL_APP_PASSWORD?.trim();
  if (gmailUser && gmailPass) {
    return {
      transporter: nodemailer.createTransport({
        service: "gmail",
        auth: { user: gmailUser, pass: gmailPass },
      }),
      from: `"AAILA Family" <${gmailUser}>`,
    };
  }

  const host = process.env.SMTP_HOST?.trim();
  const port = Number(process.env.SMTP_PORT || 587);
  const user = process.env.SMTP_USER?.trim();
  const pass = process.env.SMTP_PASS?.trim();
  if (host && user && pass) {
    return {
      transporter: nodemailer.createTransport({
        host,
        port,
        secure: port === 465,
        auth: { user, pass },
      }),
      from: process.env.SMTP_FROM?.trim() || `"AAILA Family" <${user}>`,
    };
  }

  return null;
}

export async function generateAndSendEmailOtp(rawEmail, purpose = "register") {
  const email = normalizeEmail(rawEmail);
  if (!isValidEmail(email)) {
    throw Object.assign(new Error("Please enter a valid email address."), { statusCode: 400 });
  }

  const mailer = createTransporter();
  if (!mailer) {
    throw Object.assign(
      new Error("Email OTP service is not configured on server (set GMAIL_USER & GMAIL_APP_PASSWORD in Vercel)."),
      { statusCode: 503 }
    );
  }

  const otp = String(crypto.randomInt(100000, 1000000));
  const otpHash = await bcrypt.hash(otp, 8);
  const now = Date.now();

  const firestore = getFirestore();
  await firestore.collection("email_otps").doc(email).set({
    email,
    purpose,
    otpHash,
    attempts: 0,
    createdAt: new Date(now).toISOString(),
    expiresAtMs: now + OTP_EXPIRY_MS,
  });

  const actionLabel = purpose === "reset" ? "Password Reset" : "Account Verification";
  await mailer.transporter.sendMail({
    from: mailer.from,
    to: email,
    subject: `AAILA ${actionLabel} OTP: ${otp}`,
    text: `Your AAILA ${actionLabel} OTP is ${otp}. It is valid for 10 minutes. Do not share this code with anyone.`,
    html: `<div style="font-family:sans-serif;max-width:460px;margin:auto;padding:20px;border:1px solid #e5e7eb;border-radius:12px;">
      <h2 style="color:#1e3a8a;margin-top:0;">AAILA ${actionLabel}</h2>
      <p>Use this 6-digit OTP to continue:</p>
      <div style="font-size:32px;font-weight:bold;letter-spacing:6px;color:#0f172a;background:#f1f5f9;padding:14px 20px;border-radius:8px;text-align:center;">${otp}</div>
      <p style="color:#64748b;font-size:13px;margin-top:16px;">This code expires in 10 minutes. If you didn't request this, you can safely ignore this email.</p>
    </div>`,
  });

  return { email, expiresInMinutes: 10 };
}

export async function verifyStoredEmailOtp(rawEmail, rawOtp, expectedPurpose) {
  const email = normalizeEmail(rawEmail);
  const otp = String(rawOtp || "").trim();

  if (!isValidEmail(email)) {
    throw Object.assign(new Error("Please enter a valid email address."), { statusCode: 400 });
  }
  if (!/^\d{6}$/.test(otp)) {
    throw Object.assign(new Error("Please enter a valid 6-digit OTP."), { statusCode: 400 });
  }

  const firestore = getFirestore();
  const docRef = firestore.collection("email_otps").doc(email);
  const doc = await docRef.get();

  if (!doc.exists) {
    throw Object.assign(new Error("OTP not found or expired. Please request a new OTP."), { statusCode: 400 });
  }

  const data = doc.data();
  if (expectedPurpose && data.purpose && data.purpose !== expectedPurpose) {
    throw Object.assign(new Error("OTP purpose mismatch. Please request a new OTP."), { statusCode: 400 });
  }
  if (Date.now() > Number(data.expiresAtMs || 0)) {
    await docRef.delete().catch(() => {});
    throw Object.assign(new Error("OTP expired. Please request a new OTP."), { statusCode: 400 });
  }
  if (Number(data.attempts || 0) >= MAX_OTP_ATTEMPTS) {
    await docRef.delete().catch(() => {});
    throw Object.assign(new Error("Too many wrong OTP attempts. Please request a new OTP."), { statusCode: 429 });
  }

  const match = await bcrypt.compare(otp, data.otpHash || "");
  if (!match) {
    await docRef.update({ attempts: Number(data.attempts || 0) + 1 }).catch(() => {});
    throw Object.assign(new Error("Invalid OTP code. Please check and try again."), { statusCode: 401 });
  }

  await docRef.delete().catch(() => {});

  const secret = process.env.JWT_SECRET?.trim();
  if (!secret) {
    throw new Error("JWT_SECRET environment variable is not set.");
  }

  return jwt.sign({ email, purpose: expectedPurpose || data.purpose, type: "email_otp" }, secret, {
    expiresIn: "15m",
  });
}

export function verifyOtpProofToken(token, expectedEmail, expectedPurpose) {
  const secret = process.env.JWT_SECRET?.trim();
  if (!secret || !token) return null;
  try {
    const decoded = jwt.verify(token, secret);
    if (decoded?.type !== "email_otp") return null;
    if (expectedEmail && normalizeEmail(decoded.email) !== normalizeEmail(expectedEmail)) return null;
    if (expectedPurpose && decoded.purpose !== expectedPurpose) return null;
    return normalizeEmail(decoded.email);
  } catch (_err) {
    return null;
  }
}
