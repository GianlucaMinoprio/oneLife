package com.onelife.verify;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class SelfUrlBuilder {
    public static String makeQrUrl(String backendBase, String configId, UUID mcUuid) {
        String sessionId = UUID.randomUUID().toString();
        String stateJson = String.format("{\"minecraftUuid\":\"%s\",\"sessionId\":\"%s\"}", mcUuid, sessionId);
        String stateB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(stateJson.getBytes(StandardCharsets.UTF_8));
        return backendBase + "/scan?configId=" + configId + "&state=" + stateB64;
    }
}
