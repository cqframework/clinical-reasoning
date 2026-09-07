package org.opencds.cqf.fhir.utility;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.regex.Pattern;

/** Deterministic logical IDs for newly generated resources; never normalize supplied IDs or references. */
public final class GeneratedIds {
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9.-]{1,64}");
    private static final Pattern INVALID_CHARACTER = Pattern.compile("[^A-Za-z0-9.-]");
    private static final String HEX = "0123456789abcdef";

    private GeneratedIds() {}

    /** Preserve legal IDs; otherwise retain a readable stem and a digest of the complete UTF-8 input. */
    public static String fromComposite(String original) {
        Objects.requireNonNull(original, "original");
        if (VALID_ID.matcher(original).matches()) {
            return original;
        }
        final byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(original.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
        var hash = new StringBuilder(12);
        for (int i = 0; i < 6; i++) {
            hash.append(HEX.charAt((digest[i] >>> 4) & 0xf));
            hash.append(HEX.charAt(digest[i] & 0xf));
        }
        var stem = INVALID_CHARACTER.matcher(original).replaceAll("");
        if (stem.length() > 51) {
            stem = stem.substring(0, 51);
        }
        return stem.isEmpty() ? hash.toString() : stem + "-" + hash;
    }
}
