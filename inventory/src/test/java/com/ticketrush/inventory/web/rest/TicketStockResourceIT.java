package com.ticketrush.inventory.web.rest;

import static com.ticketrush.inventory.domain.TicketStockAsserts.*;
import static com.ticketrush.inventory.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.inventory.IntegrationTest;
import com.ticketrush.inventory.security.AuthoritiesConstants;
import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.repository.TicketStockRepository;
import com.ticketrush.inventory.service.dto.TicketStockDTO;
import com.ticketrush.inventory.service.mapper.TicketStockMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link TicketStockResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class TicketStockResourceIT {

    private static final Long DEFAULT_TICKET_TYPE_ID = 1L;
    private static final Long UPDATED_TICKET_TYPE_ID = 2L;

    private static final Long DEFAULT_EVENT_ID = 1L;
    private static final Long UPDATED_EVENT_ID = 2L;

    private static final Integer DEFAULT_TOTAL_QUANTITY = 0;
    private static final Integer UPDATED_TOTAL_QUANTITY = 1;

    private static final Integer DEFAULT_AVAILABLE_QUANTITY = 0;
    private static final Integer UPDATED_AVAILABLE_QUANTITY = 1;

    private static final String ENTITY_API_URL = "/api/ticket-stocks";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TicketStockRepository ticketStockRepository;

    @Autowired
    private TicketStockMapper ticketStockMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTicketStockMockMvc;

    private TicketStock ticketStock;

    private TicketStock insertedTicketStock;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TicketStock createEntity() {
        return new TicketStock()
            .ticketTypeId(DEFAULT_TICKET_TYPE_ID)
            .eventId(DEFAULT_EVENT_ID)
            .totalQuantity(DEFAULT_TOTAL_QUANTITY)
            .availableQuantity(DEFAULT_AVAILABLE_QUANTITY);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TicketStock createUpdatedEntity() {
        return new TicketStock()
            .ticketTypeId(UPDATED_TICKET_TYPE_ID)
            .eventId(UPDATED_EVENT_ID)
            .totalQuantity(UPDATED_TOTAL_QUANTITY)
            .availableQuantity(UPDATED_AVAILABLE_QUANTITY);
    }

    @BeforeEach
    void initTest() {
        ticketStock = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedTicketStock != null) {
            // By id: the test may have bumped the @Version, so the instance we inserted is stale
            ticketStockRepository.deleteById(insertedTicketStock.getId());
            insertedTicketStock = null;
        }
    }

    @Test
    @Transactional
    void createTicketStock() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the TicketStock
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);
        var returnedTicketStockDTO = om.readValue(
            restTicketStockMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketStockDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            TicketStockDTO.class
        );

        // Validate the TicketStock in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedTicketStock = ticketStockMapper.toEntity(returnedTicketStockDTO);
        assertTicketStockUpdatableFieldsEquals(returnedTicketStock, getPersistedTicketStock(returnedTicketStock));

        insertedTicketStock = returnedTicketStock;
    }

    @Test
    @Transactional
    void createTicketStockWithExistingId() throws Exception {
        // Create the TicketStock with an existing ID
        ticketStock.setId(1L);
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restTicketStockMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TicketStock in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTicketTypeIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ticketStock.setTicketTypeId(null);

        // Create the TicketStock, which fails.
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        restTicketStockMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEventIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ticketStock.setEventId(null);

        // Create the TicketStock, which fails.
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        restTicketStockMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTotalQuantityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ticketStock.setTotalQuantity(null);

        // Create the TicketStock, which fails.
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        restTicketStockMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAvailableQuantityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ticketStock.setAvailableQuantity(null);

        // Create the TicketStock, which fails.
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        restTicketStockMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllTicketStocks() throws Exception {
        // Initialize the database
        insertedTicketStock = ticketStockRepository.saveAndFlush(ticketStock);

        // Get all the ticketStockList
        restTicketStockMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(ticketStock.getId().intValue())))
            .andExpect(jsonPath("$.[*].ticketTypeId").value(hasItem(DEFAULT_TICKET_TYPE_ID.intValue())))
            .andExpect(jsonPath("$.[*].eventId").value(hasItem(DEFAULT_EVENT_ID.intValue())))
            .andExpect(jsonPath("$.[*].totalQuantity").value(hasItem(DEFAULT_TOTAL_QUANTITY)))
            .andExpect(jsonPath("$.[*].availableQuantity").value(hasItem(DEFAULT_AVAILABLE_QUANTITY)));
    }

    @Test
    @Transactional
    void getTicketStock() throws Exception {
        // Initialize the database
        insertedTicketStock = ticketStockRepository.saveAndFlush(ticketStock);

        // Get the ticketStock
        restTicketStockMockMvc
            .perform(get(ENTITY_API_URL_ID, ticketStock.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(ticketStock.getId().intValue()))
            .andExpect(jsonPath("$.ticketTypeId").value(DEFAULT_TICKET_TYPE_ID.intValue()))
            .andExpect(jsonPath("$.eventId").value(DEFAULT_EVENT_ID.intValue()))
            .andExpect(jsonPath("$.totalQuantity").value(DEFAULT_TOTAL_QUANTITY))
            .andExpect(jsonPath("$.availableQuantity").value(DEFAULT_AVAILABLE_QUANTITY));
    }

    @Test
    @Transactional
    void getNonExistingTicketStock() throws Exception {
        // Get the ticketStock
        restTicketStockMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingTicketStock() throws Exception {
        // Initialize the database
        insertedTicketStock = ticketStockRepository.saveAndFlush(ticketStock);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ticketStock
        TicketStock updatedTicketStock = ticketStockRepository.findById(ticketStock.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTicketStock are not directly saved in db
        em.detach(updatedTicketStock);
        updatedTicketStock
            .ticketTypeId(UPDATED_TICKET_TYPE_ID)
            .eventId(UPDATED_EVENT_ID)
            .totalQuantity(UPDATED_TOTAL_QUANTITY)
            .availableQuantity(UPDATED_AVAILABLE_QUANTITY);
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(updatedTicketStock);

        restTicketStockMockMvc
            .perform(
                put(ENTITY_API_URL_ID, ticketStockDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isOk());

        // Validate the TicketStock in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedTicketStockToMatchAllProperties(updatedTicketStock);
    }

    @Test
    @Transactional
    void putNonExistingTicketStock() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketStock.setId(longCount.incrementAndGet());

        // Create the TicketStock
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTicketStockMockMvc
            .perform(
                put(ENTITY_API_URL_ID, ticketStockDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TicketStock in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchTicketStock() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketStock.setId(longCount.incrementAndGet());

        // Create the TicketStock
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTicketStockMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TicketStock in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamTicketStock() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketStock.setId(longCount.incrementAndGet());

        // Create the TicketStock
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTicketStockMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketStockDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the TicketStock in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateTicketStockWithPatch() throws Exception {
        // Initialize the database
        insertedTicketStock = ticketStockRepository.saveAndFlush(ticketStock);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ticketStock using partial update
        TicketStock partialUpdatedTicketStock = new TicketStock();
        partialUpdatedTicketStock.setId(ticketStock.getId());

        partialUpdatedTicketStock
            .ticketTypeId(UPDATED_TICKET_TYPE_ID)
            .totalQuantity(UPDATED_TOTAL_QUANTITY)
            .availableQuantity(UPDATED_AVAILABLE_QUANTITY);

        restTicketStockMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTicketStock.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTicketStock))
            )
            .andExpect(status().isOk());

        // Validate the TicketStock in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTicketStockUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedTicketStock, ticketStock),
            getPersistedTicketStock(ticketStock)
        );
    }

    @Test
    @Transactional
    void fullUpdateTicketStockWithPatch() throws Exception {
        // Initialize the database
        insertedTicketStock = ticketStockRepository.saveAndFlush(ticketStock);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ticketStock using partial update
        TicketStock partialUpdatedTicketStock = new TicketStock();
        partialUpdatedTicketStock.setId(ticketStock.getId());

        partialUpdatedTicketStock
            .ticketTypeId(UPDATED_TICKET_TYPE_ID)
            .eventId(UPDATED_EVENT_ID)
            .totalQuantity(UPDATED_TOTAL_QUANTITY)
            .availableQuantity(UPDATED_AVAILABLE_QUANTITY);

        restTicketStockMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTicketStock.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTicketStock))
            )
            .andExpect(status().isOk());

        // Validate the TicketStock in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTicketStockUpdatableFieldsEquals(partialUpdatedTicketStock, getPersistedTicketStock(partialUpdatedTicketStock));
    }

    @Test
    @Transactional
    void patchNonExistingTicketStock() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketStock.setId(longCount.incrementAndGet());

        // Create the TicketStock
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTicketStockMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, ticketStockDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TicketStock in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchTicketStock() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketStock.setId(longCount.incrementAndGet());

        // Create the TicketStock
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTicketStockMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TicketStock in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamTicketStock() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketStock.setId(longCount.incrementAndGet());

        // Create the TicketStock
        TicketStockDTO ticketStockDTO = ticketStockMapper.toDto(ticketStock);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTicketStockMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(ticketStockDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the TicketStock in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteTicketStock() throws Exception {
        // Initialize the database
        insertedTicketStock = ticketStockRepository.saveAndFlush(ticketStock);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the ticketStock
        restTicketStockMockMvc
            .perform(delete(ENTITY_API_URL_ID, ticketStock.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return ticketStockRepository.count();
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

    protected TicketStock getPersistedTicketStock(TicketStock ticketStock) {
        return ticketStockRepository.findById(ticketStock.getId()).orElseThrow();
    }

    protected void assertPersistedTicketStockToMatchAllProperties(TicketStock expectedTicketStock) {
        assertTicketStockAllPropertiesEquals(expectedTicketStock, getPersistedTicketStock(expectedTicketStock));
    }

    protected void assertPersistedTicketStockToMatchUpdatableProperties(TicketStock expectedTicketStock) {
        assertTicketStockAllUpdatablePropertiesEquals(expectedTicketStock, getPersistedTicketStock(expectedTicketStock));
    }
}
