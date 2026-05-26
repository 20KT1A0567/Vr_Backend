package com.vrtechnologies.vrtech.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

public class TotpUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_LENGTH = 6;
    private static final int KEY_SIZE_BYTES = 10; // 80 bits, standard for TOTP secrets

    public static String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[KEY_SIZE_BYTES];
        random.nextBytes(bytes);
        return Base32.encode(bytes);
    }

    public static String getQrCodeUrl(String secretKey, String email, String issuer) {
        String normalizedIssuer = issuer.replace(" ", "%20");
        String normalizedEmail = email.replace(" ", "%20");
        return "otpauth://totp/" + normalizedIssuer + ":" + normalizedEmail + "?secret=" + secretKey + "&issuer=" + normalizedIssuer;
    }

    public static boolean verifyCode(String secretKey, String codeStr, int windowWidth) {
        if (codeStr == null || codeStr.isBlank()) {
            return false;
        }
        String cleanCode = codeStr.trim();
        if (cleanCode.length() != CODE_LENGTH) {
            return false;
        }
        int code;
        try {
            code = Integer.parseInt(cleanCode);
        } catch (NumberFormatException e) {
            return false;
        }

        byte[] decodedKey;
        try {
            decodedKey = Base32.decode(secretKey);
        } catch (IllegalArgumentException e) {
            return false;
        }

        long timeWindow = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

        for (int i = -windowWidth; i <= windowWidth; i++) {
            long hash = getCodeForTime(decodedKey, timeWindow + i);
            if (hash == code) {
                return true;
            }
        }
        return false;
    }

    private static long getCodeForTime(byte[] key, long time) {
        byte[] data = ByteBuffer.allocate(8).putLong(time).array();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xf;
            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xff);
            }
            truncatedHash &= 0x7fffffff;
            truncatedHash %= 1000000;
            return truncatedHash;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Error generating TOTP", e);
        }
    }

    public static class Base32 {
        private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        private static final int[] DECODE_TABLE = new int[128];

        static {
            Arrays.fill(DECODE_TABLE, -1);
            for (int i = 0; i < ALPHABET.length(); i++) {
                DECODE_TABLE[ALPHABET.charAt(i)] = i;
                DECODE_TABLE[Character.toLowerCase(ALPHABET.charAt(i))] = i;
            }
        }

        public static String encode(byte[] data) {
            StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
            int val = 0;
            int count = 0;
            for (byte b : data) {
                val = (val << 8) | (b & 0xff);
                count += 8;
                while (count >= 5) {
                    sb.append(ALPHABET.charAt((val >>> (count - 5)) & 0x1f));
                    count -= 5;
                }
            }
            if (count > 0) {
                sb.append(ALPHABET.charAt((val << (5 - count)) & 0x1f));
            }
            return sb.toString();
        }

        public static byte[] decode(String base32) {
            String clean = base32.replaceAll("[\\s\\-=]", "");
            int length = clean.length();
            int outLength = length * 5 / 8;
            byte[] out = new byte[outLength];
            int val = 0;
            int count = 0;
            int index = 0;
            for (int i = 0; i < length; i++) {
                int c = clean.charAt(i);
                if (c >= DECODE_TABLE.length || DECODE_TABLE[c] < 0) {
                    throw new IllegalArgumentException("Invalid Base32 character: " + (char) c);
                }
                val = (val << 5) | DECODE_TABLE[c];
                count += 5;
                if (count >= 8) {
                    out[index++] = (byte) (val >>> (count - 8));
                    count -= 8;
                }
            }
            return out;
        }
    }
}
