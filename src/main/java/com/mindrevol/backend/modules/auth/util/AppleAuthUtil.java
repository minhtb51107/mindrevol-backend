package com.mindrevol.backend.modules.auth.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleAuthUtil {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final Map<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();

    public Claims validateToken(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode headerNode = objectMapper.readTree(headerJson);
            String kid = headerNode.get("kid").asText();

            PublicKey publicKey = getPublicKey(kid);

            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(identityToken)
                    .getBody();

        } catch (Exception e) {
            log.error("Apple Token Validation Failed: {}", e.getMessage());
            throw new RuntimeException("Invalid Apple Identity Token");
        }
    }

    private PublicKey getPublicKey(String kid) throws Exception {
        if (publicKeyCache.containsKey(kid)) {
            return publicKeyCache.get(kid);
        }

        log.info("Fetching Apple Public Keys...");
        String response = restTemplate.getForObject(APPLE_KEYS_URL, String.class);
        JsonNode keys = objectMapper.readTree(response).get("keys");

        for (JsonNode key : keys) {
            String keyId = key.get("kid").asText();
            String modulus = key.get("n").asText();
            String exponent = key.get("e").asText();

            BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(modulus));
            BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(exponent));
            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PublicKey pub = factory.generatePublic(spec);

            publicKeyCache.put(keyId, pub);
        }

        if (!publicKeyCache.containsKey(kid)) {
            throw new RuntimeException("Apple Public Key not found for kid: " + kid);
        }
        return publicKeyCache.get(kid);
    }
}