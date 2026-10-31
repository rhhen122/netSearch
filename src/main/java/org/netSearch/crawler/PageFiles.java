package org.netSearch.crawler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class PageFiles {

    private PageFiles() {
    }

    static String hashUrl(String url) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        for (byte value : hash) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }
}
