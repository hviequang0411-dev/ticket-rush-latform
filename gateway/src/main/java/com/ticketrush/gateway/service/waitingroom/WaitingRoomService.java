package com.ticketrush.gateway.service.waitingroom;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * A virtual queue per event, in Redis, shared by every gateway instance.
 * <p>
 * Joining hands out the next number. An admission pointer ("served") moves forward at {@code admitPerSecond},
 * computed from the time since it last moved, so it advances at the same rate however many instances or pollers
 * touch it, and an idle queue does not bank capacity. Numbers at or below the pointer are admitted and get a
 * short-lived pass, signed with a key shared with the order service, which refuses checkouts for waiting-room
 * events without one. Skipping the queue by calling the API directly therefore does not work.
 */
@Service
public class WaitingRoomService {

    /**
     * KEYS: seq, tickets (hash user -> number), served, tick. ARGV: user, now millis, rate per second, join (1/0), ttl seconds.
     * Returns {number or -1, served, last number handed out}.
     */
    static final RedisScript<List> SCRIPT = RedisScript.of(
        """
        local number = redis.call('HGET', KEYS[2], ARGV[1])
        if (not number) and ARGV[4] == '1' then
          number = redis.call('INCR', KEYS[1])
          redis.call('HSET', KEYS[2], ARGV[1], number)
        end
        local now = tonumber(ARGV[2])
        local rate = tonumber(ARGV[3])
        local last = tonumber(redis.call('GET', KEYS[1]) or '0')
        local served = tonumber(redis.call('GET', KEYS[3]) or '0')
        local tickRaw = redis.call('GET', KEYS[4])
        local tick = tickRaw and tonumber(tickRaw) or now
        local add = math.floor((now - tick) * rate / 1000)
        served = math.min(served + add, last)
        if served >= last then
          tick = now
        elseif add > 0 then
          tick = tick + math.floor(add * 1000 / rate)
        end
        redis.call('SET', KEYS[3], served)
        redis.call('SET', KEYS[4], tick)
        for i = 1, 4 do redis.call('EXPIRE', KEYS[i], tonumber(ARGV[5])) end
        return {tonumber(number or -1), served, last}
        """,
        List.class
    );

    private static final Duration KEY_TTL = Duration.ofHours(24);

    private final ReactiveStringRedisTemplate redis;

    private final Clock clock;

    private final int admitPerSecond;

    private final Duration passTtl;

    private final byte[] secret;

    public WaitingRoomService(
        ReactiveStringRedisTemplate redis,
        Clock clock,
        @Value("${ticketrush.waiting-room.admit-per-second:20}") int admitPerSecond,
        @Value("${ticketrush.waiting-room.pass-ttl:PT10M}") Duration passTtl,
        @Value("${ticketrush.waiting-room.pass-secret:waiting-room-dev-secret}") String secret
    ) {
        this.redis = redis;
        this.clock = clock;
        this.admitPerSecond = admitPerSecond;
        this.passTtl = passTtl;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Takes a number (keeping the one already held) and reports where the buyer stands. */
    public Mono<QueueStatus> join(long eventId, String user) {
        return run(eventId, user, true).map(state -> status(eventId, user, state));
    }

    /** Where the buyer stands; {@code inQueue} is false if they never joined. */
    public Mono<QueueStatus> status(long eventId, String user) {
        return run(eventId, user, false).map(state -> status(eventId, user, state));
    }

    public Mono<QueueStats> stats(long eventId) {
        return run(eventId, "", false).map(state -> new QueueStats(eventId, state[2] - state[1], state[1], state[2], admitPerSecond));
    }

    private Mono<long[]> run(long eventId, String user, boolean join) {
        String prefix = "wr:" + eventId + ":";
        List<String> keys = List.of(prefix + "seq", prefix + "tickets", prefix + "served", prefix + "tick");
        List<String> args = List.of(
            user,
            String.valueOf(clock.millis()),
            String.valueOf(admitPerSecond),
            join ? "1" : "0",
            String.valueOf(KEY_TTL.toSeconds())
        );
        return redis
            .execute(SCRIPT, keys, args)
            .next()
            .map(result -> new long[] { toLong(result.get(0)), toLong(result.get(1)), toLong(result.get(2)) });
    }

    private QueueStatus status(long eventId, String user, long[] state) {
        long number = state[0];
        long served = state[1];
        if (number < 0) {
            return new QueueStatus(eventId, false, false, 0, 0, null, null);
        }
        if (number <= served) {
            Instant expires = clock.instant().plus(passTtl);
            return new QueueStatus(eventId, true, true, 0, 0, pass(eventId, user, expires), expires);
        }
        long ahead = number - served - 1;
        long etaSeconds = (long) Math.ceil((double) (number - served) / admitPerSecond);
        return new QueueStatus(eventId, true, false, ahead, etaSeconds, null, null);
    }

    /** {@code <eventId>.<base64url(user)>.<expiresEpochSecond>.<signature>}; verified by the order service. */
    String pass(long eventId, String user, Instant expires) {
        String body =
            eventId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(user.getBytes(StandardCharsets.UTF_8)) + "." + expires.getEpochSecond();
        return body + "." + sign(body);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, 16));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    /**
     * @param ahead buyers in front who are not admitted yet.
     * @param pass send as {@code X-Queue-Pass} on checkout; set once admitted.
     */
    public record QueueStatus(
        long eventId,
        boolean inQueue,
        boolean admitted,
        long ahead,
        long etaSeconds,
        String pass,
        Instant passExpiresAt
    ) {}

    /**
     * @param waiting joined but not admitted yet.
     */
    public record QueueStats(long eventId, long waiting, long admitted, long joined, int admitPerSecond) {}
}
