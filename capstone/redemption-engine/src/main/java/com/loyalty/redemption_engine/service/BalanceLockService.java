package com.loyalty.redemption_engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis Distributed Lock — tránh race condition khi có nhiều concurrent redemption.
 * Theo DD-03 Section 3.1: SET lock:member:bal:{member_id} NX EX 10
 * FR-03-024: Re-validate balance if concurrent request detected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceLockService {

    private static final String LOCK_PREFIX = "lock:member:bal:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate redisTemplate;

    /**
     * Cố gắng acquire lock cho member.
     * @return true nếu lock được, false nếu đang bị lock bởi request khác.
     */
    public boolean tryLock(String memberId, String orderId) {
        String key = LOCK_PREFIX + memberId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, orderId, LOCK_TTL);
        if (Boolean.TRUE.equals(acquired)) {
            log.info("[Balance Lock] Acquired lock for member {} (order: {})", memberId, orderId);
            return true;
        }
        log.warn("[Balance Lock] Lock BUSY for member {} — concurrent request detected!", memberId);
        return false;
    }

    /**
     * Release lock sau khi xử lý xong.
     */
    public void releaseLock(String memberId) {
        String key = LOCK_PREFIX + memberId;
        redisTemplate.delete(key);
        log.info("[Balance Lock] Released lock for member {}", memberId);
    }
}
