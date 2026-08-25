package com.ET.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long";
    private static final long ACCESS_EXPIRY  = 900_000L;
    private static final long REFRESH_EXPIRY = 604_800_000L;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
    }

    @Test
    void generateAccessToken_shouldReturnNonNullToken() {
        String token = jwtTokenProvider.generateAccessToken("user@test.com", 1L, "USER");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateAccessToken_shouldContainThreeParts() {

        String token = jwtTokenProvider.generateAccessToken("user@test.com", 1L, "USER");
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        String email = "user@test.com";
        String token = jwtTokenProvider.generateAccessToken(email, 1L, "USER");
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void extractUserId_shouldReturnCorrectUserId() {
        String token = jwtTokenProvider.generateAccessToken("user@test.com", 42L, "USER");
        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void extractTokenType_accessToken_shouldReturnAccess() {
        String token = jwtTokenProvider.generateAccessToken("user@test.com", 1L, "USER");
        assertThat(jwtTokenProvider.extractTokenType(token)).isEqualTo("access");
    }

    @Test
    void generateRefreshToken_shouldReturnNonNullToken() {
        String token = jwtTokenProvider.generateRefreshToken("user@test.com");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractTokenType_refreshToken_shouldReturnRefresh() {
        String token = jwtTokenProvider.generateRefreshToken("user@test.com");
        assertThat(jwtTokenProvider.extractTokenType(token)).isEqualTo("refresh");
    }

    @Test
    void validateToken_validToken_shouldReturnTrue() {
        String token = jwtTokenProvider.generateAccessToken("user@test.com", 1L, "USER");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_shouldReturnFalse() {

        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1L, -1L);
        String token = expiredProvider.generateAccessToken("user@test.com", 1L, "USER");
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_tamperedToken_shouldReturnFalse() {
        String token = jwtTokenProvider.generateAccessToken("user@test.com", 1L, "USER");

        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalidsignature";
        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    void validateToken_randomString_shouldReturnFalse() {
        assertThat(jwtTokenProvider.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void validateToken_emptyString_shouldReturnFalse() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    void tokens_forDifferentUsers_shouldBeDifferent() {
        String token1 = jwtTokenProvider.generateAccessToken("user1@test.com", 1L, "USER");
        String token2 = jwtTokenProvider.generateAccessToken("user2@test.com", 2L, "USER");
        assertThat(token1).isNotEqualTo(token2);
    }
}
