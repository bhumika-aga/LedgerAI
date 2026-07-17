package com.ledgerai.auth;

import com.ledgerai.auth.config.AuthProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Issues short-lived access tokens (SECURITY §7, ADR-001).
 *
 * <p>
 * The token carries only the minimal claim needed to identify the user — the
 * subject is the user
 * id — plus issued/expiry timestamps. No roles, permissions, or PII are
 * embedded.
 */
@Service
public class JwtService {
    
    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;
    
    public JwtService(JwtEncoder jwtEncoder, AuthProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }
    
    public String issueAccessToken(UUID userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                                  .subject(userId.toString())
                                  .issuedAt(now)
                                  .expiresAt(now.plus(properties.jwt().accessTokenTtl()))
                                  .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
    
    public long accessTokenTtlSeconds() {
        return properties.jwt().accessTokenTtl().toSeconds();
    }
}
