package com.quizzmosis.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void correctPasswordIsAccepted() {
        String hash = PasswordHasher.hash("correct horse battery staple");

        assertTrue(PasswordHasher.verify("correct horse battery staple", hash));
    }

    @Test
    void wrongPasswordIsRejected() {
        String hash = PasswordHasher.hash("secret-password");

        assertFalse(PasswordHasher.verify("Secret-password", hash), "passwords are case-sensitive");
        assertFalse(PasswordHasher.verify("secret-password ", hash));
        assertFalse(PasswordHasher.verify("", hash));
    }

    @Test
    void samePasswordGetsADifferentHashEveryTime() {
        String first = PasswordHasher.hash("password123");
        String second = PasswordHasher.hash("password123");

        assertNotEquals(first, second, "every hash has its own random salt");
        assertTrue(PasswordHasher.verify("password123", first));
        assertTrue(PasswordHasher.verify("password123", second));
    }

    @Test
    void hashHasTheDocumentedFormatAndHidesThePassword() {
        String hash = PasswordHasher.hash("password123");
        String[] parts = hash.split("\\$");

        assertEquals(4, parts.length, "algorithm$iterations$salt$hash");
        assertEquals("pbkdf2_sha256", parts[0]);
        assertEquals("600000", parts[1]);
        assertFalse(hash.contains("password123"));
    }

    @Test
    void missingOrMalformedHashesAreRejectedInsteadOfThrowing() {
        assertFalse(PasswordHasher.verify("password123", null));
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("password123")));
        assertFalse(PasswordHasher.verify("password123", ""));
        assertFalse(PasswordHasher.verify("password123", "password123"), "a plain-text password is not a hash");
        assertFalse(PasswordHasher.verify("password123", "md5$1$abc$def"));
        assertFalse(PasswordHasher.verify("password123", "pbkdf2_sha256$many$abc$def"));
        assertFalse(PasswordHasher.verify("password123", "pbkdf2_sha256$1000$not base64!$def"));
    }

    /** The README promises that every demo account uses "password123"; this keeps that promise true. */
    @Test
    void demoAccountsInSeedDataUseTheDocumentedPassword() throws IOException {
        String seed = Files.readString(Path.of("db", "seed.sql"));
        Matcher hashes = Pattern.compile("'(pbkdf2_sha256\\$[^']+)'").matcher(seed);

        int count = 0;
        while (hashes.find()) {
            assertTrue(PasswordHasher.verify("password123", hashes.group(1)), "hash " + hashes.group(1));
            count++;
        }
        assertEquals(7, count, "number of demo accounts in db/seed.sql");
    }
}
