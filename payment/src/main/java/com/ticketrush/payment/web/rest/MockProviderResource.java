package com.ticketrush.payment.web.rest;

import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import com.ticketrush.payment.service.flow.PaymentFlowService;
import com.ticketrush.payment.service.flow.PaymentView;
import com.ticketrush.payment.service.provider.MockPaymentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    public MockProviderResource(PaymentFlowService paymentFlowService, MockPaymentProvider mockPaymentProvider) {
        this.paymentFlowService = paymentFlowService;
        this.mockPaymentProvider = mockPaymentProvider;
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
     * Sends the signed callback for the chosen outcome through the regular webhook path.
     */
    @PostMapping("/{providerRef}/complete")
    public PaymentView complete(@PathVariable String providerRef, @RequestParam(defaultValue = MockPaymentProvider.SUCCESS) String result) {
        PaymentView payment = find(providerRef);
        paymentFlowService.handleWebhook(PaymentProvider.MOCK, mockPaymentProvider.callbackFor(payment.providerRef(), payment.amount(), result), null);
        return find(providerRef);
    }

    private PaymentView find(String providerRef) {
        return ResponseUtil.wrapOrNotFound(paymentFlowService.findByProviderRef(providerRef)).getBody();
    }
}
