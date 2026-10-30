package com.ticketrush.order.service.ticket;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ticket codes and the QR payload printed on them: {@code TR1.<code>.<signature>}. The signature is an HMAC of the
 * code, so a scanner rejects made-up or edited codes without trusting anything the holder shows. Guessing a valid
 * payload means guessing 128 bits.
 */
@Component
public class TicketCodes {

    private static final String PREFIX = "TR1.";

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private final SecureRandom random = new SecureRandom();

    private final byte[] secret;

    public TicketCodes(@Value("${ticketrush.tickets.signing-secret:ticket-signing-dev-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String newCode() {
        char[] code = new char[12];
        for (int i = 0; i < code.length; i++) {
            code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return "TK" + new String(code);
    }

    public String qrPayload(String code) {
        return PREFIX + code + "." + sign(code);
    }

    /**
     * @return the ticket code if the payload is well formed and signed by us.
     */
    public Optional<String> verify(String payload) {
        if (payload == null || !payload.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String[] parts = payload.substring(PREFIX.length()).split("\\.", -1);
        if (parts.length != 2 || parts[0].isEmpty()) {
            return Optional.empty();
        }
        boolean signed = MessageDigest.isEqual(sign(parts[0]).getBytes(StandardCharsets.US_ASCII), parts[1].getBytes(StandardCharsets.US_ASCII));
        return signed ? Optional.of(parts[0]) : Optional.empty();
    }

    private String sign(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
            // 128 bits are plenty for a QR code and keep it small
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, 16));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
