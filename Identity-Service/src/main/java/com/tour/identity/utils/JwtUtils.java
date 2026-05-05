package com.tour.identity.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.tour.identity.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Component
public class JwtUtils {
    @Value("${jwt.signerKey}")
    private String SIGNER_KEY;

    // Token ngắn hạn cho mỗi Request
    public String generateAccessToken(User user) {
        return buildToken(user, 1, ChronoUnit.HOURS, false);
    }

    // Token dài hạn để đổi lấy Access Token mới
    public String generateRefreshToken(User user) {
        return buildToken(user, 24, ChronoUnit.HOURS, true);
    }

    private String buildToken(User user, long duration, ChronoUnit unit, boolean isRefresh) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet.Builder claimBuilder = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("gotour.com")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(duration, unit).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString()) // JTI  quản lý Token
                .claim("userId", user.getId());

        // Refresh Token không cần scope để giữ dung lượng nhẹ
        if (!isRefresh) {
            StringJoiner scopeJoiner = new StringJoiner(" ");
            if (user.getRoles() != null) {
                user.getRoles().forEach(role -> {
                    scopeJoiner.add("ROLE_" + role.getName());
                    if (role.getPermissions() != null) {
                        role.getPermissions().forEach(p -> scopeJoiner.add(p.getCode()));
                    }
                });
            }
            claimBuilder.claim("scope", scopeJoiner.toString());
        }

        Payload payload = new Payload(claimBuilder.build().toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error signing token", e);
        }
    }
}