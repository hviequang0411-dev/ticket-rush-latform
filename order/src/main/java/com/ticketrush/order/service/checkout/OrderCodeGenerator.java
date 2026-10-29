package com.ticketrush.order.service.checkout;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Short, unguessable order codes such as {@code TR7K2M9QXA4C}. Crockford base32: no I, L, O or U to misread.
 */
@Component
public class OrderCodeGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private final SecureRandom random = new SecureRandom();

    public String next() {
        char[] code = new char[10];
        for (int i = 0; i < code.length; i++) {
            code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return "TR" + new String(code);
    }
}
