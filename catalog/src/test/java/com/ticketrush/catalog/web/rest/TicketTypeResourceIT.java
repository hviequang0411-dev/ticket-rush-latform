package com.ticketrush.catalog.web.rest;

import static com.ticketrush.catalog.domain.TicketTypeAsserts.*;
import static com.ticketrush.catalog.web.rest.TestUtil.createUpdateProxyForBean;
import static com.ticketrush.catalog.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.catalog.IntegrationTest;
import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.TicketType;
import com.ticketrush.catalog.repository.TicketTypeRepository;
import com.ticketrush.catalog.service.TicketTypeService;
import com.ticketrush.catalog.service.dto.TicketTypeDTO;
import com.ticketrush.catalog.service.mapper.TicketTypeMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link TicketTypeResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class TicketTypeResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal(0);
    private static final BigDecimal UPDATED_PRICE = new BigDecimal(1);

    private static final String DEFAULT_CURRENCY = "AAA";
    private static final String UPDATED_CURRENCY = "BBB";

    private static final Integer DEFAULT_TOTAL_QUANTITY = 1;
    private static final Integer UPDATED_TOTAL_QUANTITY = 2;

    private static final Instant DEFAULT_SALE_START = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SALE_START = Instant.ofEpochMilli(1704040426520L);

    private static final Instant DEFAULT_SALE_END = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SALE_END = Instant.ofEpochMilli(1704040426520L);

    private static final String ENTITY_API_URL = "/api/ticket-types";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepositoryMock;

    @Autowired
    private TicketTypeMapper ticketTypeMapper;

    @Mock
    private TicketTypeService ticketTypeServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTicketTypeMockMvc;

    private TicketType ticketType;

    private TicketType insertedTicketType;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TicketType createEntity(EntityManager em) {
        TicketType ticketType = new TicketType()
            .name(DEFAULT_NAME)
            .price(DEFAULT_PRICE)
            .currency(DEFAULT_CURRENCY)
            .totalQuantity(DEFAULT_TOTAL_QUANTITY)
            .saleStart(DEFAULT_SALE_START)
            .saleEnd(DEFAULT_SALE_END);
        // Add required entity
        Event event;
        if (TestUtil.findAll(em, Event.class).isEmpty()) {
            event = EventResourceIT.createEntity(em);
            em.persist(event);
            em.flush();
        } else {
            event = TestUtil.findAll(em, Event.class).getFirst();
        }
        ticketType.setEvent(event);
        return ticketType;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TicketType createUpdatedEntity(EntityManager em) {
        TicketType updatedTicketType = new TicketType()
            .name(UPDATED_NAME)
            .price(UPDATED_PRICE)
            .currency(UPDATED_CURRENCY)
            .totalQuantity(UPDATED_TOTAL_QUANTITY)
            .saleStart(UPDATED_SALE_START)
            .saleEnd(UPDATED_SALE_END);
        // Add required entity
        Event event;
        if (TestUtil.findAll(em, Event.class).isEmpty()) {
            event = EventResourceIT.createUpdatedEntity(em);
            em.persist(event);
            em.flush();
        } else {
            event = TestUtil.findAll(em, Event.class).getFirst();
        }
        updatedTicketType.setEvent(event);
        return updatedTicketType;
    }

    @BeforeEach
    void initTest() {
        ticketType = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedTicketType != null) {
            ticketTypeRepository.delete(insertedTicketType);
            insertedTicketType = null;
        }
    }

    @Test
    @Transactional
    void createTicketType() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the TicketType
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);
        var returnedTicketTypeDTO = om.readValue(
            restTicketTypeMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketTypeDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            TicketTypeDTO.class
        );

        // Validate the TicketType in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedTicketType = ticketTypeMapper.toEntity(returnedTicketTypeDTO);
        assertTicketTypeUpdatableFieldsEquals(returnedTicketType, getPersistedTicketType(returnedTicketType));

        insertedTicketType = returnedTicketType;
    }

    @Test
    @Transactional
    void createTicketTypeWithExistingId() throws Exception {
        // Create the TicketType with an existing ID
        ticketType.setId(1L);
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restTicketTypeMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketTypeDTO)))
            .andExpect(status().isBadRequest());

        // Validate the TicketType in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ticketType.setName(null);

        // Create the TicketType, which fails.
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        restTicketTypeMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketTypeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPriceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ticketType.setPrice(null);

        // Create the TicketType, which fails.
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        restTicketTypeMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketTypeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCurrencyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ticketType.setCurrency(null);

        // Create the TicketType, which fails.
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        restTicketTypeMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketTypeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTotalQuantityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ticketType.setTotalQuantity(null);

        // Create the TicketType, which fails.
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        restTicketTypeMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketTypeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllTicketTypes() throws Exception {
        // Initialize the database
        insertedTicketType = ticketTypeRepository.saveAndFlush(ticketType);

        // Get all the ticketTypeList
        restTicketTypeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(ticketType.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].price").value(hasItem(sameNumber(DEFAULT_PRICE))))
            .andExpect(jsonPath("$.[*].currency").value(hasItem(DEFAULT_CURRENCY)))
            .andExpect(jsonPath("$.[*].totalQuantity").value(hasItem(DEFAULT_TOTAL_QUANTITY)))
            .andExpect(jsonPath("$.[*].saleStart").value(hasItem(DEFAULT_SALE_START.toString())))
            .andExpect(jsonPath("$.[*].saleEnd").value(hasItem(DEFAULT_SALE_END.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTicketTypesWithEagerRelationshipsIsEnabled() throws Exception {
        when(ticketTypeServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restTicketTypeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(ticketTypeServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTicketTypesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(ticketTypeServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restTicketTypeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(ticketTypeRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getTicketType() throws Exception {
        // Initialize the database
        insertedTicketType = ticketTypeRepository.saveAndFlush(ticketType);

        // Get the ticketType
        restTicketTypeMockMvc
            .perform(get(ENTITY_API_URL_ID, ticketType.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(ticketType.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.price").value(sameNumber(DEFAULT_PRICE)))
            .andExpect(jsonPath("$.currency").value(DEFAULT_CURRENCY))
            .andExpect(jsonPath("$.totalQuantity").value(DEFAULT_TOTAL_QUANTITY))
            .andExpect(jsonPath("$.saleStart").value(DEFAULT_SALE_START.toString()))
            .andExpect(jsonPath("$.saleEnd").value(DEFAULT_SALE_END.toString()));
    }

    @Test
    @Transactional
    void getNonExistingTicketType() throws Exception {
        // Get the ticketType
        restTicketTypeMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingTicketType() throws Exception {
        // Initialize the database
        insertedTicketType = ticketTypeRepository.saveAndFlush(ticketType);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ticketType
        TicketType updatedTicketType = ticketTypeRepository.findById(ticketType.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTicketType are not directly saved in db
        em.detach(updatedTicketType);
        updatedTicketType
            .name(UPDATED_NAME)
            .price(UPDATED_PRICE)
            .currency(UPDATED_CURRENCY)
            .totalQuantity(UPDATED_TOTAL_QUANTITY)
            .saleStart(UPDATED_SALE_START)
            .saleEnd(UPDATED_SALE_END);
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(updatedTicketType);

        restTicketTypeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, ticketTypeDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(ticketTypeDTO))
            )
            .andExpect(status().isOk());

        // Validate the TicketType in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedTicketTypeToMatchAllProperties(updatedTicketType);
    }

    @Test
    @Transactional
    void putNonExistingTicketType() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketType.setId(longCount.incrementAndGet());

        // Create the TicketType
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTicketTypeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, ticketTypeDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(ticketTypeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TicketType in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchTicketType() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketType.setId(longCount.incrementAndGet());

        // Create the TicketType
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTicketTypeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(ticketTypeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TicketType in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamTicketType() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketType.setId(longCount.incrementAndGet());

        // Create the TicketType
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTicketTypeMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ticketTypeDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the TicketType in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateTicketTypeWithPatch() throws Exception {
        // Initialize the database
        insertedTicketType = ticketTypeRepository.saveAndFlush(ticketType);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ticketType using partial update
        TicketType partialUpdatedTicketType = new TicketType();
        partialUpdatedTicketType.setId(ticketType.getId());

        partialUpdatedTicketType.price(UPDATED_PRICE).currency(UPDATED_CURRENCY).totalQuantity(UPDATED_TOTAL_QUANTITY);

        restTicketTypeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTicketType.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTicketType))
            )
            .andExpect(status().isOk());

        // Validate the TicketType in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTicketTypeUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedTicketType, ticketType),
            getPersistedTicketType(ticketType)
        );
    }

    @Test
    @Transactional
    void fullUpdateTicketTypeWithPatch() throws Exception {
        // Initialize the database
        insertedTicketType = ticketTypeRepository.saveAndFlush(ticketType);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ticketType using partial update
        TicketType partialUpdatedTicketType = new TicketType();
        partialUpdatedTicketType.setId(ticketType.getId());

        partialUpdatedTicketType
            .name(UPDATED_NAME)
            .price(UPDATED_PRICE)
            .currency(UPDATED_CURRENCY)
            .totalQuantity(UPDATED_TOTAL_QUANTITY)
            .saleStart(UPDATED_SALE_START)
            .saleEnd(UPDATED_SALE_END);

        restTicketTypeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTicketType.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTicketType))
            )
            .andExpect(status().isOk());

        // Validate the TicketType in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTicketTypeUpdatableFieldsEquals(partialUpdatedTicketType, getPersistedTicketType(partialUpdatedTicketType));
    }

    @Test
    @Transactional
    void patchNonExistingTicketType() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketType.setId(longCount.incrementAndGet());

        // Create the TicketType
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTicketTypeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, ticketTypeDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(ticketTypeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TicketType in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchTicketType() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketType.setId(longCount.incrementAndGet());

        // Create the TicketType
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTicketTypeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(ticketTypeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TicketType in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamTicketType() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ticketType.setId(longCount.incrementAndGet());

        // Create the TicketType
        TicketTypeDTO ticketTypeDTO = ticketTypeMapper.toDto(ticketType);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTicketTypeMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(ticketTypeDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the TicketType in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteTicketType() throws Exception {
        // Initialize the database
        insertedTicketType = ticketTypeRepository.saveAndFlush(ticketType);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the ticketType
        restTicketTypeMockMvc
            .perform(delete(ENTITY_API_URL_ID, ticketType.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return ticketTypeRepository.count();
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

    protected TicketType getPersistedTicketType(TicketType ticketType) {
        return ticketTypeRepository.findById(ticketType.getId()).orElseThrow();
    }

    protected void assertPersistedTicketTypeToMatchAllProperties(TicketType expectedTicketType) {
        assertTicketTypeAllPropertiesEquals(expectedTicketType, getPersistedTicketType(expectedTicketType));
    }

    protected void assertPersistedTicketTypeToMatchUpdatableProperties(TicketType expectedTicketType) {
        assertTicketTypeAllUpdatablePropertiesEquals(expectedTicketType, getPersistedTicketType(expectedTicketType));
    }
}
