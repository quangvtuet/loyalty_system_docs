package com.loyalty.earning_engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final long TTL_HOURS = 24;

    public boolean isDuplicate(String sourceTxnId, String programId) {
        String key = generateHash(sourceTxnId + ":" + programId);
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", Duration.ofHours(TTL_HOURS));
        if (Boolean.TRUE.equals(isNew)) {
            log.debug("New event detected for key: {}", key);
            return false;
        }
        log.warn("Duplicate event detected for key: {}", key);
        return true;
    }

    public String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes());
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
