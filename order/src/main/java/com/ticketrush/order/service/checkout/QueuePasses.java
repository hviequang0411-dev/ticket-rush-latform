package com.ticketrush.order.service.checkout;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks the admission passes the gateway's waiting room hands out:
 * {@code <eventId>.<base64url(user)>.<expiresEpochSecond>.<signature>}, signed with a key shared with the gateway.
 * A pass is only good for its own event and buyer, until it expires.
 */
@Component
public class QueuePasses {

    private final byte[] secret;

    private final Clock clock;

    public QueuePasses(@Value("${ticketrush.waiting-room.pass-secret:waiting-room-dev-secret}") String secret, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public boolean admits(String pass, Long eventId, String userLogin) {
        if (pass == null || eventId == null || userLogin == null) {
            return false;
        }
        String[] parts = pass.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        String signed = parts[0] + "." + parts[1] + "." + parts[2];
        if (!MessageDigest.isEqual(sign(signed).getBytes(StandardCharsets.US_ASCII), parts[3].getBytes(StandardCharsets.US_ASCII))) {
            return false;
        }
        try {
            String user = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long expires = Long.parseLong(parts[2]);
            return parts[0].equals(eventId.toString()) && user.equals(userLogin) && clock.instant().getEpochSecond() < expires;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, 16));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
