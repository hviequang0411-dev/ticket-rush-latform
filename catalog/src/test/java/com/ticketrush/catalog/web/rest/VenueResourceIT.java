package com.ticketrush.catalog.web.rest;

import static com.ticketrush.catalog.domain.VenueAsserts.*;
import static com.ticketrush.catalog.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.catalog.IntegrationTest;
import com.ticketrush.catalog.domain.Venue;
import com.ticketrush.catalog.repository.VenueRepository;
import com.ticketrush.catalog.service.dto.VenueDTO;
import com.ticketrush.catalog.service.mapper.VenueMapper;
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
 * Integration tests for the {@link VenueResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class VenueResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_ADDRESS = "BBBBBBBBBB";

    private static final String DEFAULT_CITY = "AAAAAAAAAA";
    private static final String UPDATED_CITY = "BBBBBBBBBB";

    private static final Integer DEFAULT_CAPACITY = 1;
    private static final Integer UPDATED_CAPACITY = 2;

    private static final String ENTITY_API_URL = "/api/venues";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restVenueMockMvc;

    private Venue venue;

    private Venue insertedVenue;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Venue createEntity() {
        return new Venue().name(DEFAULT_NAME).address(DEFAULT_ADDRESS).city(DEFAULT_CITY).capacity(DEFAULT_CAPACITY);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Venue createUpdatedEntity() {
        return new Venue().name(UPDATED_NAME).address(UPDATED_ADDRESS).city(UPDATED_CITY).capacity(UPDATED_CAPACITY);
    }

    @BeforeEach
    void initTest() {
        venue = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedVenue != null) {
            venueRepository.delete(insertedVenue);
            insertedVenue = null;
        }
    }

    @Test
    @Transactional
    void createVenue() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Venue
        VenueDTO venueDTO = venueMapper.toDto(venue);
        var returnedVenueDTO = om.readValue(
            restVenueMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(venueDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            VenueDTO.class
        );

        // Validate the Venue in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedVenue = venueMapper.toEntity(returnedVenueDTO);
        assertVenueUpdatableFieldsEquals(returnedVenue, getPersistedVenue(returnedVenue));

        insertedVenue = returnedVenue;
    }

    @Test
    @Transactional
    void createVenueWithExistingId() throws Exception {
        // Create the Venue with an existing ID
        venue.setId(1L);
        VenueDTO venueDTO = venueMapper.toDto(venue);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restVenueMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(venueDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Venue in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        venue.setName(null);

        // Create the Venue, which fails.
        VenueDTO venueDTO = venueMapper.toDto(venue);

        restVenueMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(venueDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAddressIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        venue.setAddress(null);

        // Create the Venue, which fails.
        VenueDTO venueDTO = venueMapper.toDto(venue);

        restVenueMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(venueDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        venue.setCity(null);

        // Create the Venue, which fails.
        VenueDTO venueDTO = venueMapper.toDto(venue);

        restVenueMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(venueDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCapacityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        venue.setCapacity(null);

        // Create the Venue, which fails.
        VenueDTO venueDTO = venueMapper.toDto(venue);

        restVenueMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(venueDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllVenues() throws Exception {
        // Initialize the database
        insertedVenue = venueRepository.saveAndFlush(venue);

        // Get all the venueList
        restVenueMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(venue.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
            .andExpect(jsonPath("$.[*].city").value(hasItem(DEFAULT_CITY)))
            .andExpect(jsonPath("$.[*].capacity").value(hasItem(DEFAULT_CAPACITY)));
    }

    @Test
    @Transactional
    void getVenue() throws Exception {
        // Initialize the database
        insertedVenue = venueRepository.saveAndFlush(venue);

        // Get the venue
        restVenueMockMvc
            .perform(get(ENTITY_API_URL_ID, venue.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(venue.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.address").value(DEFAULT_ADDRESS))
            .andExpect(jsonPath("$.city").value(DEFAULT_CITY))
            .andExpect(jsonPath("$.capacity").value(DEFAULT_CAPACITY));
    }

    @Test
    @Transactional
    void getNonExistingVenue() throws Exception {
        // Get the venue
        restVenueMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingVenue() throws Exception {
        // Initialize the database
        insertedVenue = venueRepository.saveAndFlush(venue);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the venue
        Venue updatedVenue = venueRepository.findById(venue.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedVenue are not directly saved in db
        em.detach(updatedVenue);
        updatedVenue.name(UPDATED_NAME).address(UPDATED_ADDRESS).city(UPDATED_CITY).capacity(UPDATED_CAPACITY);
        VenueDTO venueDTO = venueMapper.toDto(updatedVenue);

        restVenueMockMvc
            .perform(
                put(ENTITY_API_URL_ID, venueDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(venueDTO))
            )
            .andExpect(status().isOk());

        // Validate the Venue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedVenueToMatchAllProperties(updatedVenue);
    }

    @Test
    @Transactional
    void putNonExistingVenue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        venue.setId(longCount.incrementAndGet());

        // Create the Venue
        VenueDTO venueDTO = venueMapper.toDto(venue);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restVenueMockMvc
            .perform(
                put(ENTITY_API_URL_ID, venueDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(venueDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Venue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchVenue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        venue.setId(longCount.incrementAndGet());

        // Create the Venue
        VenueDTO venueDTO = venueMapper.toDto(venue);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVenueMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(venueDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Venue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamVenue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        venue.setId(longCount.incrementAndGet());

        // Create the Venue
        VenueDTO venueDTO = venueMapper.toDto(venue);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVenueMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(venueDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Venue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateVenueWithPatch() throws Exception {
        // Initialize the database
        insertedVenue = venueRepository.saveAndFlush(venue);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the venue using partial update
        Venue partialUpdatedVenue = new Venue();
        partialUpdatedVenue.setId(venue.getId());

        partialUpdatedVenue.address(UPDATED_ADDRESS).city(UPDATED_CITY);

        restVenueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedVenue.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedVenue))
            )
            .andExpect(status().isOk());

        // Validate the Venue in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertVenueUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedVenue, venue), getPersistedVenue(venue));
    }

    @Test
    @Transactional
    void fullUpdateVenueWithPatch() throws Exception {
        // Initialize the database
        insertedVenue = venueRepository.saveAndFlush(venue);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the venue using partial update
        Venue partialUpdatedVenue = new Venue();
        partialUpdatedVenue.setId(venue.getId());

        partialUpdatedVenue.name(UPDATED_NAME).address(UPDATED_ADDRESS).city(UPDATED_CITY).capacity(UPDATED_CAPACITY);

        restVenueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedVenue.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedVenue))
            )
            .andExpect(status().isOk());

        // Validate the Venue in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertVenueUpdatableFieldsEquals(partialUpdatedVenue, getPersistedVenue(partialUpdatedVenue));
    }

    @Test
    @Transactional
    void patchNonExistingVenue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        venue.setId(longCount.incrementAndGet());

        // Create the Venue
        VenueDTO venueDTO = venueMapper.toDto(venue);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restVenueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, venueDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(venueDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Venue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchVenue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        venue.setId(longCount.incrementAndGet());

        // Create the Venue
        VenueDTO venueDTO = venueMapper.toDto(venue);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVenueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(venueDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Venue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamVenue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        venue.setId(longCount.incrementAndGet());

        // Create the Venue
        VenueDTO venueDTO = venueMapper.toDto(venue);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVenueMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(venueDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Venue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteVenue() throws Exception {
        // Initialize the database
        insertedVenue = venueRepository.saveAndFlush(venue);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the venue
        restVenueMockMvc
            .perform(delete(ENTITY_API_URL_ID, venue.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return venueRepository.count();
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

    protected Venue getPersistedVenue(Venue venue) {
        return venueRepository.findById(venue.getId()).orElseThrow();
    }

    protected void assertPersistedVenueToMatchAllProperties(Venue expectedVenue) {
        assertVenueAllPropertiesEquals(expectedVenue, getPersistedVenue(expectedVenue));
    }

    protected void assertPersistedVenueToMatchUpdatableProperties(Venue expectedVenue) {
        assertVenueAllUpdatablePropertiesEquals(expectedVenue, getPersistedVenue(expectedVenue));
    }
}
