package com.ticketrush.payment.web.rest;

import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import com.ticketrush.payment.service.flow.PaymentFlowService;
import com.ticketrush.payment.service.flow.PaymentView;
import com.ticketrush.payment.service.provider.MockPaymentProvider;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import tech.jhipster.web.util.ResponseUtil;

/**
 * The mock provider's hosted payment page. A real provider runs this page on its own site; here the buyer picks
 * an outcome and the page sends the same kind of signed callback a provider would.
 * Disabled with {@code ticketrush.payment.mock.enabled=false}.
 */
@RestController
@RequestMapping("/api/public/mock-provider")
@ConditionalOnProperty(prefix = "ticketrush.payment.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockProviderResource {

    private final PaymentFlowService paymentFlowService;

    private final MockPaymentProvider mockPaymentProvider;

    private final String returnUrl;

    public MockProviderResource(
        PaymentFlowService paymentFlowService,
        MockPaymentProvider mockPaymentProvider,
        @Value("${ticketrush.payment.mock.return-url:/order-result?orderCode={orderCode}}") String returnUrl
    ) {
        this.paymentFlowService = paymentFlowService;
        this.mockPaymentProvider = mockPaymentProvider;
        this.returnUrl = returnUrl;
    }

    @GetMapping(value = "/{providerRef}", produces = MediaType.TEXT_HTML_VALUE)
    public String page(@PathVariable String providerRef) {
        PaymentView payment = find(providerRef);
        String ref = HtmlUtils.htmlEscape(payment.providerRef());
        return (
            "<!doctype html><html><head><meta charset=\"utf-8\"><title>Mock payment</title></head><body>" +
            "<h1>Mock payment provider</h1>" +
            "<p>Payment " + ref + ": " + payment.amount().toPlainString() + " " +
            HtmlUtils.htmlEscape(payment.currency()) + " (" + payment.status() + ")</p>" +
            "<form method=\"post\" action=\"" + ref + "/complete?result=success\"><button>Pay</button></form>" +
            "<form method=\"post\" action=\"" + ref + "/complete?result=declined\"><button>Decline</button></form>" +
            "</body></html>"
        );
    }

    /**
     * Sends the signed callback for the chosen outcome through the regular webhook path. A browser (the page's form)
     * is then sent back to the shop like a real provider would do; API clients get the payment as JSON.
     */
    @PostMapping("/{providerRef}/complete")
    public ResponseEntity<PaymentView> complete(
        @PathVariable String providerRef,
        @RequestParam(defaultValue = MockPaymentProvider.SUCCESS) String result,
        @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept
    ) {
        PaymentView payment = find(providerRef);
        paymentFlowService.handleWebhook(PaymentProvider.MOCK, mockPaymentProvider.callbackFor(payment.providerRef(), payment.amount(), result), null);
        if (accept != null && accept.contains(MediaType.TEXT_HTML_VALUE)) {
            String location = returnUrl.replace("{orderCode}", URLEncoder.encode(payment.orderCode(), StandardCharsets.UTF_8));
            return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(location)).build();
        }
        return ResponseEntity.ok(find(providerRef));
    }

    private PaymentView find(String providerRef) {
        return ResponseUtil.wrapOrNotFound(paymentFlowService.findByProviderRef(providerRef)).getBody();
    }
}
