package com.ticketrush.payment.web.rest;

import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import com.ticketrush.payment.service.flow.PaymentFlowService;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * Callbacks from payment providers. Public (providers have no token); trust comes from each provider's signature,
 * checked before anything is applied.
 */
@RestController
@RequestMapping("/api/public/webhooks")
public class PaymentWebhookResource {

    private final PaymentFlowService paymentFlowService;

    public PaymentWebhookResource(PaymentFlowService paymentFlowService) {
        this.paymentFlowService = paymentFlowService;
    }

    /**
     * {@code GET|POST /api/public/webhooks/{provider}} : parameters as query/form fields, or a raw body. Some
     * providers (VNPay IPN) call with GET.
     *
     * @return 200 once stored and applied (also for a redelivery), 400 for a bad signature.
     */
    @RequestMapping(value = "/{provider}", method = { RequestMethod.GET, RequestMethod.POST })
    public Map<String, String> receive(
        @PathVariable String provider,
        @RequestParam Map<String, String> params,
        @RequestBody(required = false) String body
    ) {
        paymentFlowService.handleWebhook(PaymentProvider.valueOf(provider.toUpperCase(Locale.ROOT)), params, body);
        return Map.of("status", "ok");
    }
}
