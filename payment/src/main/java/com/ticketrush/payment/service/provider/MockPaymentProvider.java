package com.ticketrush.payment.service.provider;

import com.ticketrush.payment.domain.Payment;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A stand-in for a real provider so the whole flow runs locally and in CI without merchant accounts.
 * <p>
 * It behaves like one: the buyer is sent to a hosted page (served by this service), and the result comes back as
 * a signed callback through the same webhook path, signature check and idempotency as VNPay or MoMo would use.
 */
@Component
public class MockPaymentProvider implements PaymentProviderGateway {

    public static final String SUCCESS = "success";

    private static final Logger LOG = LoggerFactory.getLogger(MockPaymentProvider.class);

    private final String publicBaseUrl;

    private final byte[] secret;

    public MockPaymentProvider(
        @Value("${ticketrush.payment.public-base-url:http://localhost:8080}") String publicBaseUrl,
        @Value("${ticketrush.payment.mock.secret:mock-provider-dev-secret}") String secret
    ) {
        this.publicBaseUrl = publicBaseUrl;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.MOCK;
    }

    @Override
    public String createPaymentUrl(Payment payment) {
        return publicBaseUrl + "/services/payment/api/public/mock-provider/" + payment.getProviderRef();
    }

    /**
     * What the mock provider sends back once the buyer chose an outcome on its page.
     */
    public Map<String, String> callbackFor(String providerRef, BigDecimal amount, String result) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("ref", providerRef);
        params.put("result", result);
        params.put("amount", amount.toPlainString());
        params.put("transactionId", "MOCK-" + UUID.randomUUID());
        params.put("signature", sign(params));
        return params;
    }

    @Override
    public WebhookNotification parseWebhook(Map<String, String> params, String rawBody) {
        String signature = params.getOrDefault("signature", "");
        boolean valid = MessageDigest.isEqual(sign(params).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        boolean success = SUCCESS.equals(params.get("result"));
        return new WebhookNotification(
            valid,
            params.get("ref") + ":" + params.get("transactionId"),
            params.get("ref"),
            success,
            params.get("transactionId"),
            params.containsKey("amount") ? new BigDecimal(params.get("amount")) : null,
            success ? null : "DECLINED"
        );
    }

    @Override
    public void refund(Payment payment) {
        LOG.info("Mock provider refunded {} {} for payment {}", payment.getAmount(), payment.getCurrency(), payment.getProviderRef());
    }

    private String sign(Map<String, String> params) {
        String canonical = String.join("|", params.get("ref"), params.get("result"), params.get("amount"), params.get("transactionId"));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
