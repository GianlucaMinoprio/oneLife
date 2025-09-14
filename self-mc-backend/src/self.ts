// src/self.ts
import crypto from "crypto";
// If you render QR server-side, you can build a URL here. Most teams point to a tiny web page that uses @selfxyz/qrcode.

const CONFIG_ID = process.env.SELF_CONFIG_ID!; // your configId

// Stable app-scoped nullifier from Self’s user id (privacy-preserving)
export function deriveNullifier(userRootId: string) {
  const appId = process.env.SELF_APP_ID || "mc-one-life";
  return crypto
    .createHash("sha256")
    .update(`${appId}:${userRootId}`)
    .digest("hex");
}

export const CONFIG = { CONFIG_ID };
