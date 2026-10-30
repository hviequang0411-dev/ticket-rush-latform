package com.ticketrush.payment.web.rest;

import static com.ticketrush.payment.domain.PaymentWebhookEventAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.payment.IntegrationTest;
import com.ticketrush.payment.security.AuthoritiesConstants;
import com.ticketrush.payment.domain.PaymentWebhookEvent;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import com.ticketrush.payment.repository.PaymentWebhookEventRepository;
import com.ticketrush.payment.service.mapper.PaymentWebhookEventMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link PaymentWebhookEventResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class PaymentWebhookEventResourceIT {

    private static final PaymentProvider DEFAULT_PROVIDER = PaymentProvider.MOCK;
    private static final PaymentProvider UPDATED_PROVIDER = PaymentProvider.VNPAY;

    private static final String DEFAULT_EVENT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_EVENT_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_PAYLOAD = "AAAAAAAAAA";
    private static final String UPDATED_PAYLOAD = "BBBBBBBBBB";

    private static final Boolean DEFAULT_SIGNATURE_VALID = false;
    private static final Boolean UPDATED_SIGNATURE_VALID = true;

    private static final Instant DEFAULT_RECEIVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RECEIVED_AT = Instant.ofEpochMilli(1793332893973L);

    private static final Instant DEFAULT_PROCESSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PROCESSED_AT = Instant.ofEpochMilli(1793332893973L);

    private static final String ENTITY_API_URL = "/api/payment-webhook-events";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PaymentWebhookEventRepository paymentWebhookEventRepository;

    @Autowired
    private PaymentWebhookEventMapper paymentWebhookEventMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPaymentWebhookEventMockMvc;

    private PaymentWebhookEvent paymentWebhookEvent;

    private PaymentWebhookEvent insertedPaymentWebhookEvent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PaymentWebhookEvent createEntity() {
        return new PaymentWebhookEvent()
            .provider(DEFAULT_PROVIDER)
            .eventKey(DEFAULT_EVENT_KEY)
            .payload(DEFAULT_PAYLOAD)
            .signatureValid(DEFAULT_SIGNATURE_VALID)
            .receivedAt(DEFAULT_RECEIVED_AT)
            .processedAt(DEFAULT_PROCESSED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PaymentWebhookEvent createUpdatedEntity() {
        return new PaymentWebhookEvent()
            .provider(UPDATED_PROVIDER)
            .eventKey(UPDATED_EVENT_KEY)
            .payload(UPDATED_PAYLOAD)
            .signatureValid(UPDATED_SIGNATURE_VALID)
            .receivedAt(UPDATED_RECEIVED_AT)
            .processedAt(UPDATED_PROCESSED_AT);
    }

    @BeforeEach
    void initTest() {
        paymentWebhookEvent = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedPaymentWebhookEvent != null) {
            paymentWebhookEventRepository.delete(insertedPaymentWebhookEvent);
            insertedPaymentWebhookEvent = null;
        }
    }

    @Test
    @Transactional
    void getAllPaymentWebhookEvents() throws Exception {
        // Initialize the database
        insertedPaymentWebhookEvent = paymentWebhookEventRepository.saveAndFlush(paymentWebhookEvent);

        // Get all the paymentWebhookEventList
        restPaymentWebhookEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(paymentWebhookEvent.getId().intValue())))
            .andExpect(jsonPath("$.[*].provider").value(hasItem(DEFAULT_PROVIDER.toString())))
            .andExpect(jsonPath("$.[*].eventKey").value(hasItem(DEFAULT_EVENT_KEY)))
            .andExpect(jsonPath("$.[*].payload").value(hasItem(DEFAULT_PAYLOAD)))
            .andExpect(jsonPath("$.[*].signatureValid").value(hasItem(DEFAULT_SIGNATURE_VALID)))
            .andExpect(jsonPath("$.[*].receivedAt").value(hasItem(DEFAULT_RECEIVED_AT.toString())))
            .andExpect(jsonPath("$.[*].processedAt").value(hasItem(DEFAULT_PROCESSED_AT.toString())));
    }

    @Test
    @Transactional
    void getPaymentWebhookEvent() throws Exception {
        // Initialize the database
        insertedPaymentWebhookEvent = paymentWebhookEventRepository.saveAndFlush(paymentWebhookEvent);

        // Get the paymentWebhookEvent
        restPaymentWebhookEventMockMvc
            .perform(get(ENTITY_API_URL_ID, paymentWebhookEvent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(paymentWebhookEvent.getId().intValue()))
            .andExpect(jsonPath("$.provider").value(DEFAULT_PROVIDER.toString()))
            .andExpect(jsonPath("$.eventKey").value(DEFAULT_EVENT_KEY))
            .andExpect(jsonPath("$.payload").value(DEFAULT_PAYLOAD))
            .andExpect(jsonPath("$.signatureValid").value(DEFAULT_SIGNATURE_VALID))
            .andExpect(jsonPath("$.receivedAt").value(DEFAULT_RECEIVED_AT.toString()))
            .andExpect(jsonPath("$.processedAt").value(DEFAULT_PROCESSED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingPaymentWebhookEvent() throws Exception {
        // Get the paymentWebhookEvent
        restPaymentWebhookEventMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return paymentWebhookEventRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected PaymentWebhookEvent getPersistedPaymentWebhookEvent(PaymentWebhookEvent paymentWebhookEvent) {
        return paymentWebhookEventRepository.findById(paymentWebhookEvent.getId()).orElseThrow();
    }

    protected void assertPersistedPaymentWebhookEventToMatchAllProperties(PaymentWebhookEvent expectedPaymentWebhookEvent) {
        assertPaymentWebhookEventAllPropertiesEquals(
            expectedPaymentWebhookEvent,
            getPersistedPaymentWebhookEvent(expectedPaymentWebhookEvent)
        );
    }

    protected void assertPersistedPaymentWebhookEventToMatchUpdatableProperties(PaymentWebhookEvent expectedPaymentWebhookEvent) {
        assertPaymentWebhookEventAllUpdatablePropertiesEquals(
            expectedPaymentWebhookEvent,
            getPersistedPaymentWebhookEvent(expectedPaymentWebhookEvent)
        );
    }
}
