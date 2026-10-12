package com.edupulse.util;

import com.edupulse.config.EduPulseProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** AES-256-GCM helpers for wellbeing free-text. Stable pseudonym for IDs. */
@Component
public class Crypto {
    private static final SecureRandom RNG = new SecureRandom();
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;

    public Crypto(EduPulseProperties props) {
        String hex = props.getWellbeing().getEncKeyHex();
        if (hex == null || hex.length() < 64) {
            throw new IllegalStateException("WELLBEING_ENC_KEY must be 64 hex chars (32 bytes)");
        }
        byte[] raw = hexToBytes(hex);
        if (raw.length != 32) {
            byte[] padded = new byte[32];
            System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, 32));
            raw = padded;
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] blob = new byte[IV_LEN + ct.length];
            System.arraycopy(iv, 0, blob, 0, IV_LEN);
            System.arraycopy(ct, 0, blob, IV_LEN, ct.length);
            return Base64.getEncoder().encodeToString(blob);
        } catch (Exception e) {
            throw new IllegalStateException("encrypt failed", e);
        }
    }

    public String decrypt(String payload) {
        if (payload == null) return null;
        try {
            byte[] blob = Base64.getDecoder().decode(payload);
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(blob, 0, iv, 0, IV_LEN);
            byte[] ct = new byte[blob.length - IV_LEN];
            System.arraycopy(blob, IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** Stable 12-char pseudonym — used before any Gemini call. */
    public String pseudonym(String value) {
        if (value == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(("edupulse-v1:" + value).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h).substring(0, 12);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i + 1), 16));
        }
        return out;
    }
}
