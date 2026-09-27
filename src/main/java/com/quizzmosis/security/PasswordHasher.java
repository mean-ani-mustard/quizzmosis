package com.quizzmosis.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hashes and checks passwords with PBKDF2-HMAC-SHA256 and a random salt per password.
 *
 * <p>Stored format: {@code pbkdf2_sha256$<iterations>$<salt>$<hash>} (salt and hash are Base64).
 * The iteration count is part of the string, so it can be raised later without breaking
 * existing accounts.</p>
 *
 * <p>Why not a plain SHA-256 of the password? A fast, unsalted hash lets anyone who gets hold
 * of the database look passwords up in precomputed tables or brute-force them at billions of
 * guesses per second. The salt defeats precomputed tables and the iteration count makes every
 * single guess slow.</p>
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2_sha256";
    /** OWASP's recommended work factor for PBKDF2-HMAC-SHA256. */
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** Returns a new salted hash of {@code password}, ready to be stored in the database. */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + base64.encodeToString(salt) + "$" + base64.encodeToString(hash);
    }

    /** Checks {@code password} against a hash produced by {@link #hash(String)}. */
    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations);
            // Constant-time comparison, so response times don't leak how many bytes matched.
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            // Malformed number or Base64 in the stored value.
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available in this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }
}
