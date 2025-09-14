package com.onelife.backend;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

public class BackendClient {
    private final String base;
    private final Logger log;
    private final OkHttpClient http = new OkHttpClient();

    public BackendClient(String base, Logger log) {
        this.base = base;
        this.log = log;
    }

    public static class Status {
        public final boolean verified;
        public final boolean dead;
        public final String country;

        public Status(boolean v, boolean d, String c) {
            verified = v;
            dead = d;
            country = c;
        }

        public static Status unverified() {
            return new Status(false, false, null);
        }
    }

    public static class Session {
        public final String deepLink;
        public final String qrPngDataUrl;
        public final long expiresAt;
        public final String sessionId;

        public Session(String deepLink, String qrPngDataUrl, long expiresAt, String sessionId) {
            this.deepLink = deepLink;
            this.qrPngDataUrl = qrPngDataUrl;
            this.expiresAt = expiresAt;
            this.sessionId = sessionId;
        }
    }

    public Session createSession(String mcUuid) {
        JSONObject body = new JSONObject().put("minecraftUuid", mcUuid);
        Request req = new Request.Builder()
                .url(base + "/sessions")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();
        try (Response r = http.newCall(req).execute()) {
            if (!r.isSuccessful()) {
                log.warning("createSession failed: " + r.code());
                return null;
            }
            JSONObject j = new JSONObject(r.body().string());
            String deepLink = j.optString("deepLink", j.optString("qrUrl", null));
            String qrPng = j.optString("qrPngDataUrl", null);
            long exp = j.optLong("expiresAt", 0);
            String sessionId = j.optString("sessionId", "");
            if (deepLink == null || deepLink.isEmpty()) {
                log.warning("createSession: missing deepLink in response");
                return null;
            }
            log.info("createSession ok: sessionId=" + sessionId + ", exp=" + exp);
            return new Session(deepLink, qrPng, exp, sessionId);
        } catch (IOException e) {
            log.warning("session error: " + e.getMessage());
            return null;
        }
    }

    public Status getStatus(String mcUuid) {
        Request req = new Request.Builder().url(base + "/status/" + mcUuid).build();
        try (Response r = http.newCall(req).execute()) {
            if (!r.isSuccessful())
                return Status.unverified();
            JSONObject j = new JSONObject(r.body().string());
            if (j.optBoolean("dead", false))
                return new Status(false, true, null);
            if (j.optBoolean("verified", false))
                return new Status(true, false, j.optString("country", null));
            return Status.unverified();
        } catch (IOException e) {
            log.warning("status error: " + e.getMessage());
            return Status.unverified();
        }
    }

    public void reportDeath(String mcUuid) {
        JSONObject b = new JSONObject().put("minecraftUuid", mcUuid);
        Request req = new Request.Builder()
                .url(base + "/death")
                .post(RequestBody.create(b.toString(), MediaType.get("application/json")))
                .build();
        try (Response r = http.newCall(req).execute()) {
            if (!r.isSuccessful())
                log.warning("death report failed: " + r.code());
            else
                log.info("death report sent ok for uuid=" + mcUuid);
        } catch (IOException e) {
            log.warning("death report error: " + e.getMessage());
        }
    }
}
