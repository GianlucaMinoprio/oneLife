import { Router } from "express";
import { z } from "zod";
import { Store } from "./store";
import { deriveNullifier } from "./self";
import {
  SelfBackendVerifier,
  DefaultConfigStore,
  getUniversalLink,
  AllIds,
  countryCodes,
} from "@selfxyz/core";
import QRCode from "qrcode";
import { nanoid } from "nanoid";
import { signLoginToken } from "./crypto";
import type { SelfApp } from "@selfxyz/common";

const r = Router();

// Initialize Self verifier
const SCOPE = "minecraft-poh";
const VERIFY_URL = "https://daimo.ngrok.app/self/verify";
const IS_PROD = true;
const ENDPOINT_TYPE = "https";
const CHAIN_ID = 42220;
const MOCK = false;

const configStore = new DefaultConfigStore({
  excludedCountries: [],
  ofac: false,
});

const selfVerifier = new SelfBackendVerifier(
  SCOPE,
  VERIFY_URL,
  MOCK,
  AllIds,
  configStore,
  "uuid" // Use UUID for user identifiers to match Minecraft UUIDs
);

// Create or reuse a verification session for a player
r.post("/sessions", async (req, res) => {
  const schema = z.object({ minecraftUuid: z.string().min(10) });
  const { minecraftUuid } = schema.parse(req.body);

  // If already verified
  const b = Store.getByUuid(minecraftUuid);
  if (b && b.verified_at) {
    return res.json({
      verified: true,
      country: b.country_code,
      dead: b.status === "dead_banned",
      token: signLoginToken({
        mc_uuid: minecraftUuid,
        country_code: b.country_code,
        status: "verified",
      }),
    });
  }
  // If dead
  if (b && b.status === "dead_banned") {
    return res.status(403).json({
      verified: b.verified_at !== null,
      dead: true,
      reason: "You died in this world",
    });
  }

  // Create session for Self verification flow
  const sessionId = nanoid(21);
  const expiresAt = Date.now() + 10 * 60 * 1000;

  // Build SelfApp configuration for a backend-only deep link (no frontend package)
  const selfApp: SelfApp = {
    version: 2,
    appName: process.env.SELF_APP_NAME || "Minecraft Verification",
    scope: SCOPE,
    endpoint: VERIFY_URL,
    logoBase64: "https://i.postimg.cc/mrmVf9hm/self.png",
    userId: minecraftUuid,
    deeplinkCallback: "",
    header: "",
    endpointType: ENDPOINT_TYPE,
    userIdType: "uuid",
    sessionId,
    chainID: CHAIN_ID,
    devMode: !IS_PROD,
    userDefinedData: JSON.stringify({ minecraftUuid, sessionId }),
    disclosures: {
      //minimumAge: 1,
      //   ofac: false,
      //   excludedCountries: [],
      nationality: true,
    },
  };

  const deeplink = getUniversalLink(selfApp);
  const qrPngDataUrl = await QRCode.toDataURL(deeplink, {
    errorCorrectionLevel: "M",
  });

  const session = {
    sessionId,
    verificationUrl: deeplink,
    deepLink: deeplink,
    expiresAt,
    qrPngDataUrl,
  } as const;

  // Make sure there is a row for this uuid if none yet, only for the session fields
  if (!b) {
    Store.insert({
      nullifier: `pending:${session.sessionId}`, // temporary
      mc_uuid: minecraftUuid,
      country_code: "ZZ", // unknown
      status: "bound", // will be corrected at webhook time
      created_at: Date.now(),
      verified_at: null,
      session_id: session.sessionId,
      session_expires_at: session.expiresAt,
    });
  } else {
    Store.setSession(minecraftUuid, session.sessionId, session.expiresAt);
  }

  res.json({
    sessionId: session.sessionId,
    qrUrl: session.verificationUrl,
    deepLink: session.deepLink,
    expiresAt: session.expiresAt,
    qrPngDataUrl: session.qrPngDataUrl,
  });
});

// Simple landing page the Self app can redirect back to after verification
r.get("/verified", (_req, res) => {
  res
    .type("html")
    .send(
      '<html><body style="font-family:sans-serif;"><h1>✅ Verified</h1><p>You can return to Minecraft now.</p></body></html>'
    );
});

// Poll status
r.get("/status/:uuid", (req, res) => {
  const mc_uuid = req.params.uuid;
  const b = Store.getByUuid(mc_uuid);
  if (!b) return res.json({ verified: false, dead: false });

  if (b.status === "dead_banned") {
    return res.json({ verified: false, dead: true });
  }
  if (b.verified_at) {
    return res.json({
      verified: true,
      country: b.country_code,
      token: signLoginToken({
        mc_uuid,
        country_code: b.country_code,
        status: "verified",
      }),
    });
  }
  return res.json({ verified: false, dead: false });
});

// Death hook from the plugin
r.post("/death", (req, res) => {
  const schema = z.object({ minecraftUuid: z.string().min(10) });
  const { minecraftUuid } = schema.parse(req.body);
  const b = Store.getByUuid(minecraftUuid);
  if (!b) return res.status(404).json({ error: "NOT_FOUND" });
  Store.updateStatusByUuid(minecraftUuid, "dead_banned");
  return res.json({ ok: true });
});

// Webhook from Self
r.post("/self/webhook", expressRawJson, async (req, res) => {
  // Verify signature header from Self, example 'x-self-signature'
  const sig = req.header("x-self-signature") || "";
  // TODO: call verifySelfWebhook with raw body. If your framework already parsed JSON, use req.rawBody captured by expressRawJson
  // if (!verifySelfWebhook(req.rawBody, sig)) return res.status(401).send("bad sig");

  // Expected payload example:
  // { sessionId, success, user: { id: "user_root_id" }, claims: { nationality: "AR" }, metadata: { minecraftUuid } }
  const body = req.body as any;
  if (!body?.success) return res.sendStatus(200);

  const mc_uuid = body?.metadata?.minecraftUuid;
  const country = body?.claims?.nationality;
  const userRootId = body?.user?.id;
  if (!mc_uuid || !country || !userRootId)
    return res.status(400).send("bad payload");

  const nullifier = deriveNullifier(userRootId);

  try {
    Store.upsertOnVerify({
      nullifier,
      mc_uuid,
      country_code: country,
    });
  } catch (e: any) {
    if (e.message === "NULLIFIER_ALREADY_BOUND_TO_DIFFERENT_UUID")
      return res.status(409).send("identity already used");
    if (e.message === "UUID_ALREADY_BOUND_TO_DIFFERENT_NULLIFIER")
      return res.status(409).send("uuid already has identity");
    return res.status(500).send("server error");
  }

  return res.sendStatus(200);
});

// Self proof verification endpoint
r.post("/self/verify", async (req, res) => {
  const attestationIdNum = Number(req.body?.attestationId);
  const proof = req.body?.proof;
  const pubSignals = req.body?.pubSignals || req.body?.publicSignals;
  let userContextData: string | undefined =
    req.body?.userContextData ||
    req.body?.userContextHash ||
    req.body?.userContext;

  if (typeof userContextData === "string" && userContextData.startsWith("0x")) {
    userContextData = userContextData.slice(2);
  }

  if (
    !Number.isFinite(attestationIdNum) ||
    !proof ||
    !pubSignals ||
    !userContextData
  ) {
    return res.status(400).json({ error: "Missing required fields" });
  }

  if (attestationIdNum !== 1 && attestationIdNum !== 2) {
    return res.status(400).json({ error: "Invalid attestationId" });
  }
  const attestationId = attestationIdNum as 1 | 2;

  try {
    console.log("/self/verify payload received", {
      hasAttestationId: !!attestationId,
      hasProof: !!proof,
      hasPubSignals: !!pubSignals,
      hasUserContextData: !!userContextData,
    });
    // Do not parse userContextData here; verify first, then read userDefinedData from result
    const result = await selfVerifier.verify(
      attestationId,
      proof,
      pubSignals,
      userContextData
    );
    console.log("/self/verify result", {
      isValid: result?.isValidDetails?.isValid,
      nationality: result?.discloseOutput?.nationality,
    });

    if (!result.isValidDetails.isValid) {
      return res.status(401).json({
        error: "Verification failed",
        details: result.isValidDetails,
      });
    }

    // Extract nationality from disclosed data
    const nationality = result.discloseOutput.nationality;
    const userIdentifier = result.userData.userIdentifier;
    // Decode minecraftUuid from userDefinedData (hex, 64 bytes)
    let mcUuidFromContext = "";
    try {
      const udHex: string = (result.userData as any).userDefinedData || "";
      const hex = udHex.startsWith("0x") ? udHex.slice(2) : udHex;
      let decoded = Buffer.from(hex, "hex").toString();
      decoded = decoded.replace(/\u0000+$/g, "").trim();
      const endIdx =
        decoded.indexOf("}") >= 0 ? decoded.indexOf("}") + 1 : decoded.length;
      const jsonStr = decoded.slice(0, endIdx);
      const parsed = JSON.parse(jsonStr);
      mcUuidFromContext = parsed?.minecraftUuid || "";
    } catch {}
    if (!mcUuidFromContext) {
      return res
        .status(400)
        .json({ error: "Missing minecraft UUID in user context" });
    }

    // Create nullifier from user identifier
    const nullifier = deriveNullifier(userIdentifier);

    // Store the verified binding
    try {
      Store.upsertOnVerify({
        nullifier,
        mc_uuid: mcUuidFromContext,
        country_code: nationality,
      });
    } catch (e: any) {
      if (e.message === "NULLIFIER_ALREADY_BOUND_TO_DIFFERENT_UUID")
        return res
          .status(409)
          .json({ error: "Identity already used by different player" });
      if (e.message === "UUID_ALREADY_BOUND_TO_DIFFERENT_NULLIFIER")
        return res
          .status(409)
          .json({ error: "Player already has different identity" });
      throw e;
    }

    return res.json({
      status: "success",
      result: true,
      credentialSubject: result.discloseOutput,
      verificationOptions: {
        ofac: false,
        excludedCountries: [],
      },
      token: signLoginToken({
        mc_uuid: mcUuidFromContext,
        country_code: nationality,
        status: "verified",
      }),
    });
  } catch (error: any) {
    console.error("Verification error:", error);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// capture raw body for webhook HMAC
function expressRawJson(req: any, _res: any, next: any) {
  let data = "";
  req.setEncoding("utf8");
  req.on("data", (chunk: string) => {
    data += chunk;
  });
  req.on("end", () => {
    (req as any).rawBody = data;
    try {
      req.body = data ? JSON.parse(data) : {};
    } catch {
      req.body = {};
    }
    next();
  });
}

export default r;
