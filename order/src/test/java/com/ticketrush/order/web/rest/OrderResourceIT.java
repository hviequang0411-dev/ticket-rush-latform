package com.ticketrush.order.web.rest;

import static com.ticketrush.order.domain.OrderAsserts.*;
import static com.ticketrush.order.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.order.IntegrationTest;
import com.ticketrush.order.security.AuthoritiesConstants;
import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.service.mapper.OrderMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
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
 * Integration tests for the {@link OrderResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class OrderResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_USER_LOGIN = "AAAAAAAAAA";
    private static final String UPDATED_USER_LOGIN = "BBBBBBBBBB";

    private static final OrderStatus DEFAULT_STATUS = OrderStatus.PENDING;
    private static final OrderStatus UPDATED_STATUS = OrderStatus.RESERVED;

    private static final BigDecimal DEFAULT_TOTAL_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_TOTAL_AMOUNT = new BigDecimal(1);
    private static final BigDecimal SMALLER_TOTAL_AMOUNT = new BigDecimal(0 - 1);

    private static final String DEFAULT_CURRENCY = "AAA";
    private static final String UPDATED_CURRENCY = "BBB";

    private static final String DEFAULT_IDEMPOTENCY_KEY = "AAAAAAAAAA";
    private static final String UPDATED_IDEMPOTENCY_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_FAILURE_REASON = "AAAAAAAAAA";
    private static final String UPDATED_FAILURE_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_EXPIRES_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRES_AT = Instant.ofEpochMilli(1793253677524L);

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1793253677524L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1793253677524L);

    private static final String ENTITY_API_URL = "/api/orders";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderMockMvc;

    private Order order;

    private Order insertedOrder;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Order createEntity() {
        return new Order()
            .code(DEFAULT_CODE)
            .userLogin(DEFAULT_USER_LOGIN)
            .status(DEFAULT_STATUS)
            .totalAmount(DEFAULT_TOTAL_AMOUNT)
            .currency(DEFAULT_CURRENCY)
            .idempotencyKey(DEFAULT_IDEMPOTENCY_KEY)
            .failureReason(DEFAULT_FAILURE_REASON)
            .expiresAt(DEFAULT_EXPIRES_AT)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Order createUpdatedEntity() {
        return new Order()
            .code(UPDATED_CODE)
            .userLogin(UPDATED_USER_LOGIN)
            .status(UPDATED_STATUS)
            .totalAmount(UPDATED_TOTAL_AMOUNT)
            .currency(UPDATED_CURRENCY)
            .idempotencyKey(UPDATED_IDEMPOTENCY_KEY)
            .failureReason(UPDATED_FAILURE_REASON)
            .expiresAt(UPDATED_EXPIRES_AT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        order = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedOrder != null) {
            orderRepository.delete(insertedOrder);
            insertedOrder = null;
        }
    }

    @Test
    @Transactional
    void getAllOrders() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList
        restOrderMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(order.getId().intValue())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].userLogin").value(hasItem(DEFAULT_USER_LOGIN)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].totalAmount").value(hasItem(sameNumber(DEFAULT_TOTAL_AMOUNT))))
            .andExpect(jsonPath("$.[*].currency").value(hasItem(DEFAULT_CURRENCY)))
            .andExpect(jsonPath("$.[*].idempotencyKey").value(hasItem(DEFAULT_IDEMPOTENCY_KEY)))
            .andExpect(jsonPath("$.[*].failureReason").value(hasItem(DEFAULT_FAILURE_REASON)))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getOrder() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get the order
        restOrderMockMvc
            .perform(get(ENTITY_API_URL_ID, order.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(order.getId().intValue()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.userLogin").value(DEFAULT_USER_LOGIN))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.totalAmount").value(sameNumber(DEFAULT_TOTAL_AMOUNT)))
            .andExpect(jsonPath("$.currency").value(DEFAULT_CURRENCY))
            .andExpect(jsonPath("$.idempotencyKey").value(DEFAULT_IDEMPOTENCY_KEY))
            .andExpect(jsonPath("$.failureReason").value(DEFAULT_FAILURE_REASON))
            .andExpect(jsonPath("$.expiresAt").value(DEFAULT_EXPIRES_AT.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getOrdersByIdFiltering() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        Long id = order.getId();

        defaultOrderFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultOrderFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultOrderFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllOrdersByCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where code equals to
        defaultOrderFiltering("code.equals=" + DEFAULT_CODE, "code.equals=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllOrdersByCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where code in
        defaultOrderFiltering("code.in=" + DEFAULT_CODE + "," + UPDATED_CODE, "code.in=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllOrdersByCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where code is not null
        defaultOrderFiltering("code.specified=true", "code.specified=false");
    }

    @Test
    @Transactional
    void getAllOrdersByCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where code contains
        defaultOrderFiltering("code.contains=" + DEFAULT_CODE, "code.contains=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllOrdersByCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where code does not contain
        defaultOrderFiltering("code.doesNotContain=" + UPDATED_CODE, "code.doesNotContain=" + DEFAULT_CODE);
    }

    @Test
    @Transactional
    void getAllOrdersByUserLoginIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where userLogin equals to
        defaultOrderFiltering("userLogin.equals=" + DEFAULT_USER_LOGIN, "userLogin.equals=" + UPDATED_USER_LOGIN);
    }

    @Test
    @Transactional
    void getAllOrdersByUserLoginIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where userLogin in
        defaultOrderFiltering("userLogin.in=" + DEFAULT_USER_LOGIN + "," + UPDATED_USER_LOGIN, "userLogin.in=" + UPDATED_USER_LOGIN);
    }

    @Test
    @Transactional
    void getAllOrdersByUserLoginIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where userLogin is not null
        defaultOrderFiltering("userLogin.specified=true", "userLogin.specified=false");
    }

    @Test
    @Transactional
    void getAllOrdersByUserLoginContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where userLogin contains
        defaultOrderFiltering("userLogin.contains=" + DEFAULT_USER_LOGIN, "userLogin.contains=" + UPDATED_USER_LOGIN);
    }

    @Test
    @Transactional
    void getAllOrdersByUserLoginNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where userLogin does not contain
        defaultOrderFiltering("userLogin.doesNotContain=" + UPDATED_USER_LOGIN, "userLogin.doesNotContain=" + DEFAULT_USER_LOGIN);
    }

    @Test
    @Transactional
    void getAllOrdersByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where status equals to
        defaultOrderFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllOrdersByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where status in
        defaultOrderFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllOrdersByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where status is not null
        defaultOrderFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllOrdersByTotalAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where totalAmount equals to
        defaultOrderFiltering("totalAmount.equals=" + DEFAULT_TOTAL_AMOUNT, "totalAmount.equals=" + UPDATED_TOTAL_AMOUNT);
    }

    @Test
    @Transactional
    void getAllOrdersByTotalAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where totalAmount in
        defaultOrderFiltering(
            "totalAmount.in=" + DEFAULT_TOTAL_AMOUNT + "," + UPDATED_TOTAL_AMOUNT,
            "totalAmount.in=" + UPDATED_TOTAL_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllOrdersByTotalAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where totalAmount is not null
        defaultOrderFiltering("totalAmount.specified=true", "totalAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllOrdersByTotalAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where totalAmount is greater than or equal to
        defaultOrderFiltering(
            "totalAmount.greaterThanOrEqual=" + DEFAULT_TOTAL_AMOUNT,
            "totalAmount.greaterThanOrEqual=" + UPDATED_TOTAL_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllOrdersByTotalAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where totalAmount is less than or equal to
        defaultOrderFiltering("totalAmount.lessThanOrEqual=" + DEFAULT_TOTAL_AMOUNT, "totalAmount.lessThanOrEqual=" + SMALLER_TOTAL_AMOUNT);
    }

    @Test
    @Transactional
    void getAllOrdersByTotalAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where totalAmount is less than
        defaultOrderFiltering("totalAmount.lessThan=" + UPDATED_TOTAL_AMOUNT, "totalAmount.lessThan=" + DEFAULT_TOTAL_AMOUNT);
    }

    @Test
    @Transactional
    void getAllOrdersByTotalAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where totalAmount is greater than
        defaultOrderFiltering("totalAmount.greaterThan=" + SMALLER_TOTAL_AMOUNT, "totalAmount.greaterThan=" + DEFAULT_TOTAL_AMOUNT);
    }

    @Test
    @Transactional
    void getAllOrdersByCurrencyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where currency equals to
        defaultOrderFiltering("currency.equals=" + DEFAULT_CURRENCY, "currency.equals=" + UPDATED_CURRENCY);
    }

    @Test
    @Transactional
    void getAllOrdersByCurrencyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where currency in
        defaultOrderFiltering("currency.in=" + DEFAULT_CURRENCY + "," + UPDATED_CURRENCY, "currency.in=" + UPDATED_CURRENCY);
    }

    @Test
    @Transactional
    void getAllOrdersByCurrencyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where currency is not null
        defaultOrderFiltering("currency.specified=true", "currency.specified=false");
    }

    @Test
    @Transactional
    void getAllOrdersByCurrencyContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where currency contains
        defaultOrderFiltering("currency.contains=" + DEFAULT_CURRENCY, "currency.contains=" + UPDATED_CURRENCY);
    }

    @Test
    @Transactional
    void getAllOrdersByCurrencyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where currency does not contain
        defaultOrderFiltering("currency.doesNotContain=" + UPDATED_CURRENCY, "currency.doesNotContain=" + DEFAULT_CURRENCY);
    }

    @Test
    @Transactional
    void getAllOrdersByIdempotencyKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where idempotencyKey equals to
        defaultOrderFiltering("idempotencyKey.equals=" + DEFAULT_IDEMPOTENCY_KEY, "idempotencyKey.equals=" + UPDATED_IDEMPOTENCY_KEY);
    }

    @Test
    @Transactional
    void getAllOrdersByIdempotencyKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where idempotencyKey in
        defaultOrderFiltering(
            "idempotencyKey.in=" + DEFAULT_IDEMPOTENCY_KEY + "," + UPDATED_IDEMPOTENCY_KEY,
            "idempotencyKey.in=" + UPDATED_IDEMPOTENCY_KEY
        );
    }

    @Test
    @Transactional
    void getAllOrdersByIdempotencyKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where idempotencyKey is not null
        defaultOrderFiltering("idempotencyKey.specified=true", "idempotencyKey.specified=false");
    }

    @Test
    @Transactional
    void getAllOrdersByIdempotencyKeyContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where idempotencyKey contains
        defaultOrderFiltering("idempotencyKey.contains=" + DEFAULT_IDEMPOTENCY_KEY, "idempotencyKey.contains=" + UPDATED_IDEMPOTENCY_KEY);
    }

    @Test
    @Transactional
    void getAllOrdersByIdempotencyKeyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where idempotencyKey does not contain
        defaultOrderFiltering(
            "idempotencyKey.doesNotContain=" + UPDATED_IDEMPOTENCY_KEY,
            "idempotencyKey.doesNotContain=" + DEFAULT_IDEMPOTENCY_KEY
        );
    }

    @Test
    @Transactional
    void getAllOrdersByFailureReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where failureReason equals to
        defaultOrderFiltering("failureReason.equals=" + DEFAULT_FAILURE_REASON, "failureReason.equals=" + UPDATED_FAILURE_REASON);
    }

    @Test
    @Transactional
    void getAllOrdersByFailureReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where failureReason in
        defaultOrderFiltering(
            "failureReason.in=" + DEFAULT_FAILURE_REASON + "," + UPDATED_FAILURE_REASON,
            "failureReason.in=" + UPDATED_FAILURE_REASON
        );
    }

    @Test
    @Transactional
    void getAllOrdersByFailureReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where failureReason is not null
        defaultOrderFiltering("failureReason.specified=true", "failureReason.specified=false");
    }

    @Test
    @Transactional
    void getAllOrdersByFailureReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where failureReason contains
        defaultOrderFiltering("failureReason.contains=" + DEFAULT_FAILURE_REASON, "failureReason.contains=" + UPDATED_FAILURE_REASON);
    }

    @Test
    @Transactional
    void getAllOrdersByFailureReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where failureReason does not contain
        defaultOrderFiltering(
            "failureReason.doesNotContain=" + UPDATED_FAILURE_REASON,
            "failureReason.doesNotContain=" + DEFAULT_FAILURE_REASON
        );
    }

    @Test
    @Transactional
    void getAllOrdersByExpiresAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where expiresAt equals to
        defaultOrderFiltering("expiresAt.equals=" + DEFAULT_EXPIRES_AT, "expiresAt.equals=" + UPDATED_EXPIRES_AT);
    }

    @Test
    @Transactional
    void getAllOrdersByExpiresAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where expiresAt in
        defaultOrderFiltering("expiresAt.in=" + DEFAULT_EXPIRES_AT + "," + UPDATED_EXPIRES_AT, "expiresAt.in=" + UPDATED_EXPIRES_AT);
    }

    @Test
    @Transactional
    void getAllOrdersByExpiresAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where expiresAt is not null
        defaultOrderFiltering("expiresAt.specified=true", "expiresAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOrdersByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where createdAt equals to
        defaultOrderFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllOrdersByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where createdAt in
        defaultOrderFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllOrdersByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where createdAt is not null
        defaultOrderFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOrdersByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where updatedAt equals to
        defaultOrderFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllOrdersByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where updatedAt in
        defaultOrderFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllOrdersByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrder = orderRepository.saveAndFlush(order);

        // Get all the orderList where updatedAt is not null
        defaultOrderFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    private void defaultOrderFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultOrderShouldBeFound(shouldBeFound);
        defaultOrderShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultOrderShouldBeFound(String filter) throws Exception {
        restOrderMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(order.getId().intValue())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].userLogin").value(hasItem(DEFAULT_USER_LOGIN)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].totalAmount").value(hasItem(sameNumber(DEFAULT_TOTAL_AMOUNT))))
            .andExpect(jsonPath("$.[*].currency").value(hasItem(DEFAULT_CURRENCY)))
            .andExpect(jsonPath("$.[*].idempotencyKey").value(hasItem(DEFAULT_IDEMPOTENCY_KEY)))
            .andExpect(jsonPath("$.[*].failureReason").value(hasItem(DEFAULT_FAILURE_REASON)))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restOrderMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultOrderShouldNotBeFound(String filter) throws Exception {
        restOrderMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restOrderMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingOrder() throws Exception {
        // Get the order
        restOrderMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return orderRepository.count();
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

    protected Order getPersistedOrder(Order order) {
        return orderRepository.findById(order.getId()).orElseThrow();
    }

    protected void assertPersistedOrderToMatchAllProperties(Order expectedOrder) {
        assertOrderAllPropertiesEquals(expectedOrder, getPersistedOrder(expectedOrder));
    }

    protected void assertPersistedOrderToMatchUpdatableProperties(Order expectedOrder) {
        assertOrderAllUpdatablePropertiesEquals(expectedOrder, getPersistedOrder(expectedOrder));
    }
}
