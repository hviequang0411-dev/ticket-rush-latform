package com.ticketrush.inventory.web.rest;

import static com.ticketrush.inventory.domain.StockHoldAsserts.*;
import static com.ticketrush.inventory.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.inventory.IntegrationTest;
import com.ticketrush.inventory.security.AuthoritiesConstants;
import com.ticketrush.inventory.domain.StockHold;
import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.domain.enumeration.HoldStatus;
import com.ticketrush.inventory.repository.StockHoldRepository;
import com.ticketrush.inventory.service.dto.StockHoldDTO;
import com.ticketrush.inventory.service.mapper.StockHoldMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link StockHoldResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class StockHoldResourceIT {

    private static final Long DEFAULT_ORDER_ID = 1L;
    private static final Long UPDATED_ORDER_ID = 2L;

    private static final Integer DEFAULT_QUANTITY = 1;
    private static final Integer UPDATED_QUANTITY = 2;

    private static final HoldStatus DEFAULT_STATUS = HoldStatus.HELD;
    private static final HoldStatus UPDATED_STATUS = HoldStatus.CONFIRMED;

    private static final Instant DEFAULT_EXPIRES_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRES_AT = Instant.ofEpochMilli(1793253571137L);

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1793253571137L);

    private static final String ENTITY_API_URL = "/api/stock-holds";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private StockHoldRepository stockHoldRepository;

    @Autowired
    private StockHoldMapper stockHoldMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restStockHoldMockMvc;

    private StockHold stockHold;

    private StockHold insertedStockHold;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StockHold createEntity(EntityManager em) {
        StockHold stockHold = new StockHold()
            .orderId(DEFAULT_ORDER_ID)
            .quantity(DEFAULT_QUANTITY)
            .status(DEFAULT_STATUS)
            .expiresAt(DEFAULT_EXPIRES_AT)
            .createdAt(DEFAULT_CREATED_AT);
        // Add required entity
        TicketStock ticketStock;
        if (TestUtil.findAll(em, TicketStock.class).isEmpty()) {
            ticketStock = TicketStockResourceIT.createEntity();
            em.persist(ticketStock);
            em.flush();
        } else {
            ticketStock = TestUtil.findAll(em, TicketStock.class).getFirst();
        }
        stockHold.setStock(ticketStock);
        return stockHold;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StockHold createUpdatedEntity(EntityManager em) {
        StockHold updatedStockHold = new StockHold()
            .orderId(UPDATED_ORDER_ID)
            .quantity(UPDATED_QUANTITY)
            .status(UPDATED_STATUS)
            .expiresAt(UPDATED_EXPIRES_AT)
            .createdAt(UPDATED_CREATED_AT);
        // Add required entity
        TicketStock ticketStock;
        if (TestUtil.findAll(em, TicketStock.class).isEmpty()) {
            ticketStock = TicketStockResourceIT.createUpdatedEntity();
            em.persist(ticketStock);
            em.flush();
        } else {
            ticketStock = TestUtil.findAll(em, TicketStock.class).getFirst();
        }
        updatedStockHold.setStock(ticketStock);
        return updatedStockHold;
    }

    @BeforeEach
    void initTest() {
        stockHold = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedStockHold != null) {
            stockHoldRepository.delete(insertedStockHold);
            insertedStockHold = null;
        }
    }

    @Test
    @Transactional
    void getAllStockHolds() throws Exception {
        // Initialize the database
        insertedStockHold = stockHoldRepository.saveAndFlush(stockHold);

        // Get all the stockHoldList
        restStockHoldMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(stockHold.getId().intValue())))
            .andExpect(jsonPath("$.[*].orderId").value(hasItem(DEFAULT_ORDER_ID.intValue())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @Test
    @Transactional
    void getStockHold() throws Exception {
        // Initialize the database
        insertedStockHold = stockHoldRepository.saveAndFlush(stockHold);

        // Get the stockHold
        restStockHoldMockMvc
            .perform(get(ENTITY_API_URL_ID, stockHold.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(stockHold.getId().intValue()))
            .andExpect(jsonPath("$.orderId").value(DEFAULT_ORDER_ID.intValue()))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.expiresAt").value(DEFAULT_EXPIRES_AT.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingStockHold() throws Exception {
        // Get the stockHold
        restStockHoldMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return stockHoldRepository.count();
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

    protected StockHold getPersistedStockHold(StockHold stockHold) {
        return stockHoldRepository.findById(stockHold.getId()).orElseThrow();
    }

    protected void assertPersistedStockHoldToMatchAllProperties(StockHold expectedStockHold) {
        assertStockHoldAllPropertiesEquals(expectedStockHold, getPersistedStockHold(expectedStockHold));
    }

    protected void assertPersistedStockHoldToMatchUpdatableProperties(StockHold expectedStockHold) {
        assertStockHoldAllUpdatablePropertiesEquals(expectedStockHold, getPersistedStockHold(expectedStockHold));
    }
}
