package com.tour.identity.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tour.identity.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Component
public class JwtUtils {
    @Value("${jwt.signerKey}")
    private String SIGNER_KEY;

    @Value("${jwt.accessTokenValidityInSeconds}")
    private long accessTokenValidity;

    @Value("${jwt.refreshTokenValidityInSeconds}")
    private long refreshTokenValidity;

    // Token ngắn hạn cho mỗi Request
    public String generateAccessToken(User user) {
        // Thay số 1 giờ cố định bằng biến động truyền vào qua ChronoUnit.SECONDS
        return buildToken(user, accessTokenValidity, ChronoUnit.SECONDS, false);
    }

    // Token dài hạn để đổi lấy Access Token mới
    public String generateRefreshToken(User user) {
        // Thay số 24 giờ cố định bằng biến động truyền vào qua ChronoUnit.SECONDS
        return buildToken(user, refreshTokenValidity, ChronoUnit.SECONDS, true);
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
    /**
     * Xác thực Token và trả về SignedJWT để trích xuất JTI, Expiry, Sub...
     */
    public SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        //  Parse chuỗi string thành object SignedJWT
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        //  Kiểm tra chữ ký (Signature)
        boolean verified = signedJWT.verify(verifier);
        //  Kiểm tra thời gian hết hạn (Expiration)
        boolean isExpired = signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date());

        if (!verified || isExpired) {
            throw new RuntimeException("TOKEN_INVALID_OR_EXPIRED");
        }

        return signedJWT;
    }
}