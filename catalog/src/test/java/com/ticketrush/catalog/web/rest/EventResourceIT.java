package com.ticketrush.catalog.web.rest;

import static com.ticketrush.catalog.domain.EventAsserts.*;
import static com.ticketrush.catalog.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.catalog.IntegrationTest;
import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.Venue;
import com.ticketrush.catalog.domain.enumeration.EventStatus;
import com.ticketrush.catalog.security.AuthoritiesConstants;
import com.ticketrush.catalog.repository.EventRepository;
import com.ticketrush.catalog.service.EventService;
import com.ticketrush.catalog.service.dto.EventDTO;
import com.ticketrush.catalog.service.mapper.EventMapper;
import jakarta.persistence.EntityManager;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link EventResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class EventResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "nliysv-q78iwz-0ehk-p3sxk1-uf-ioovl";
    private static final String UPDATED_SLUG = "p2nnlj-vmnwp-8z";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_START_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_TIME = Instant.ofEpochMilli(1704040426520L);

    private static final Instant DEFAULT_END_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_TIME = Instant.ofEpochMilli(1704040426520L);

    private static final EventStatus DEFAULT_STATUS = EventStatus.DRAFT;
    private static final EventStatus UPDATED_STATUS = EventStatus.PUBLISHED;

    private static final String DEFAULT_POSTER_URL = "AAAAAAAAAA";
    private static final String UPDATED_POSTER_URL = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/events";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EventRepository eventRepository;

    @Mock
    private EventRepository eventRepositoryMock;

    @Autowired
    private EventMapper eventMapper;

    @Mock
    private EventService eventServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEventMockMvc;

    private Event event;

    private Event insertedEvent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Event createEntity(EntityManager em) {
        Event event = new Event()
            .name(DEFAULT_NAME)
            .slug(DEFAULT_SLUG)
            .description(DEFAULT_DESCRIPTION)
            .startTime(DEFAULT_START_TIME)
            .endTime(DEFAULT_END_TIME)
            .status(DEFAULT_STATUS)
            .posterUrl(DEFAULT_POSTER_URL);
        // Add required entity
        Venue venue;
        if (TestUtil.findAll(em, Venue.class).isEmpty()) {
            venue = VenueResourceIT.createEntity();
            em.persist(venue);
            em.flush();
        } else {
            venue = TestUtil.findAll(em, Venue.class).getFirst();
        }
        event.setVenue(venue);
        return event;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Event createUpdatedEntity(EntityManager em) {
        Event updatedEvent = new Event()
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .startTime(UPDATED_START_TIME)
            .endTime(UPDATED_END_TIME)
            .status(UPDATED_STATUS)
            .posterUrl(UPDATED_POSTER_URL);
        // Add required entity
        Venue venue;
        if (TestUtil.findAll(em, Venue.class).isEmpty()) {
            venue = VenueResourceIT.createUpdatedEntity();
            em.persist(venue);
            em.flush();
        } else {
            venue = TestUtil.findAll(em, Venue.class).getFirst();
        }
        updatedEvent.setVenue(venue);
        return updatedEvent;
    }

    @BeforeEach
    void initTest() {
        event = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedEvent != null) {
            eventRepository.delete(insertedEvent);
            insertedEvent = null;
        }
    }

    @Test
    @Transactional
    void createEvent() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Event
        EventDTO eventDTO = eventMapper.toDto(event);
        var returnedEventDTO = om.readValue(
            restEventMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            EventDTO.class
        );

        // Validate the Event in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedEvent = eventMapper.toEntity(returnedEventDTO);
        assertEventUpdatableFieldsEquals(returnedEvent, getPersistedEvent(returnedEvent));

        insertedEvent = returnedEvent;
    }

    @Test
    @Transactional
    void createEventWithExistingId() throws Exception {
        // Create the Event with an existing ID
        event.setId(1L);
        EventDTO eventDTO = eventMapper.toDto(event);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restEventMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Event in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        event.setName(null);

        // Create the Event, which fails.
        EventDTO eventDTO = eventMapper.toDto(event);

        restEventMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        event.setSlug(null);

        // Create the Event, which fails.
        EventDTO eventDTO = eventMapper.toDto(event);

        restEventMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStartTimeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        event.setStartTime(null);

        // Create the Event, which fails.
        EventDTO eventDTO = eventMapper.toDto(event);

        restEventMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        event.setStatus(null);

        // Create the Event, which fails.
        EventDTO eventDTO = eventMapper.toDto(event);

        restEventMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllEvents() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList
        restEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(event.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].startTime").value(hasItem(DEFAULT_START_TIME.toString())))
            .andExpect(jsonPath("$.[*].endTime").value(hasItem(DEFAULT_END_TIME.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].posterUrl").value(hasItem(DEFAULT_POSTER_URL)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEventsWithEagerRelationshipsIsEnabled() throws Exception {
        when(eventServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEventMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(eventServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEventsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(eventServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEventMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(eventRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getEvent() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get the event
        restEventMockMvc
            .perform(get(ENTITY_API_URL_ID, event.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(event.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.startTime").value(DEFAULT_START_TIME.toString()))
            .andExpect(jsonPath("$.endTime").value(DEFAULT_END_TIME.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.posterUrl").value(DEFAULT_POSTER_URL));
    }

    @Test
    @Transactional
    void getEventsByIdFiltering() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        Long id = event.getId();

        defaultEventFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultEventFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultEventFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllEventsByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where name equals to
        defaultEventFiltering("name.equals=" + DEFAULT_NAME, "name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllEventsByNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where name in
        defaultEventFiltering("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME, "name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllEventsByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where name is not null
        defaultEventFiltering("name.specified=true", "name.specified=false");
    }

    @Test
    @Transactional
    void getAllEventsByNameContainsSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where name contains
        defaultEventFiltering("name.contains=" + DEFAULT_NAME, "name.contains=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllEventsByNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where name does not contain
        defaultEventFiltering("name.doesNotContain=" + UPDATED_NAME, "name.doesNotContain=" + DEFAULT_NAME);
    }

    @Test
    @Transactional
    void getAllEventsBySlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where slug equals to
        defaultEventFiltering("slug.equals=" + DEFAULT_SLUG, "slug.equals=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllEventsBySlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where slug in
        defaultEventFiltering("slug.in=" + DEFAULT_SLUG + "," + UPDATED_SLUG, "slug.in=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllEventsBySlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where slug is not null
        defaultEventFiltering("slug.specified=true", "slug.specified=false");
    }

    @Test
    @Transactional
    void getAllEventsBySlugContainsSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where slug contains
        defaultEventFiltering("slug.contains=" + DEFAULT_SLUG, "slug.contains=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllEventsBySlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where slug does not contain
        defaultEventFiltering("slug.doesNotContain=" + UPDATED_SLUG, "slug.doesNotContain=" + DEFAULT_SLUG);
    }

    @Test
    @Transactional
    void getAllEventsByStartTimeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where startTime equals to
        defaultEventFiltering("startTime.equals=" + DEFAULT_START_TIME, "startTime.equals=" + UPDATED_START_TIME);
    }

    @Test
    @Transactional
    void getAllEventsByStartTimeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where startTime in
        defaultEventFiltering("startTime.in=" + DEFAULT_START_TIME + "," + UPDATED_START_TIME, "startTime.in=" + UPDATED_START_TIME);
    }

    @Test
    @Transactional
    void getAllEventsByStartTimeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where startTime is not null
        defaultEventFiltering("startTime.specified=true", "startTime.specified=false");
    }

    @Test
    @Transactional
    void getAllEventsByEndTimeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where endTime equals to
        defaultEventFiltering("endTime.equals=" + DEFAULT_END_TIME, "endTime.equals=" + UPDATED_END_TIME);
    }

    @Test
    @Transactional
    void getAllEventsByEndTimeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where endTime in
        defaultEventFiltering("endTime.in=" + DEFAULT_END_TIME + "," + UPDATED_END_TIME, "endTime.in=" + UPDATED_END_TIME);
    }

    @Test
    @Transactional
    void getAllEventsByEndTimeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where endTime is not null
        defaultEventFiltering("endTime.specified=true", "endTime.specified=false");
    }

    @Test
    @Transactional
    void getAllEventsByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where status equals to
        defaultEventFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllEventsByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where status in
        defaultEventFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllEventsByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where status is not null
        defaultEventFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllEventsByPosterUrlIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where posterUrl equals to
        defaultEventFiltering("posterUrl.equals=" + DEFAULT_POSTER_URL, "posterUrl.equals=" + UPDATED_POSTER_URL);
    }

    @Test
    @Transactional
    void getAllEventsByPosterUrlIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where posterUrl in
        defaultEventFiltering("posterUrl.in=" + DEFAULT_POSTER_URL + "," + UPDATED_POSTER_URL, "posterUrl.in=" + UPDATED_POSTER_URL);
    }

    @Test
    @Transactional
    void getAllEventsByPosterUrlIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where posterUrl is not null
        defaultEventFiltering("posterUrl.specified=true", "posterUrl.specified=false");
    }

    @Test
    @Transactional
    void getAllEventsByPosterUrlContainsSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where posterUrl contains
        defaultEventFiltering("posterUrl.contains=" + DEFAULT_POSTER_URL, "posterUrl.contains=" + UPDATED_POSTER_URL);
    }

    @Test
    @Transactional
    void getAllEventsByPosterUrlNotContainsSomething() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        // Get all the eventList where posterUrl does not contain
        defaultEventFiltering("posterUrl.doesNotContain=" + UPDATED_POSTER_URL, "posterUrl.doesNotContain=" + DEFAULT_POSTER_URL);
    }

    @Test
    @Transactional
    void getAllEventsByVenueIsEqualToSomething() throws Exception {
        Venue venue;
        if (TestUtil.findAll(em, Venue.class).isEmpty()) {
            eventRepository.saveAndFlush(event);
            venue = VenueResourceIT.createEntity();
        } else {
            venue = TestUtil.findAll(em, Venue.class).getFirst();
        }
        em.persist(venue);
        em.flush();
        event.setVenue(venue);
        eventRepository.saveAndFlush(event);
        Long venueId = venue.getId();
        // Get all the eventList where venue equals to venueId
        defaultEventShouldBeFound("venueId.equals=" + venueId);

        // Get all the eventList where venue equals to (venueId + 1)
        defaultEventShouldNotBeFound("venueId.equals=" + (venueId + 1));
    }

    private void defaultEventFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultEventShouldBeFound(shouldBeFound);
        defaultEventShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultEventShouldBeFound(String filter) throws Exception {
        restEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(event.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].startTime").value(hasItem(DEFAULT_START_TIME.toString())))
            .andExpect(jsonPath("$.[*].endTime").value(hasItem(DEFAULT_END_TIME.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].posterUrl").value(hasItem(DEFAULT_POSTER_URL)));

        // Check, that the count call also returns 1
        restEventMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultEventShouldNotBeFound(String filter) throws Exception {
        restEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restEventMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingEvent() throws Exception {
        // Get the event
        restEventMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingEvent() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the event
        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedEvent are not directly saved in db
        em.detach(updatedEvent);
        updatedEvent
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .startTime(UPDATED_START_TIME)
            .endTime(UPDATED_END_TIME)
            .status(UPDATED_STATUS)
            .posterUrl(UPDATED_POSTER_URL);
        EventDTO eventDTO = eventMapper.toDto(updatedEvent);

        restEventMockMvc
            .perform(
                put(ENTITY_API_URL_ID, eventDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(eventDTO))
            )
            .andExpect(status().isOk());

        // Validate the Event in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedEventToMatchAllProperties(updatedEvent);
    }

    @Test
    @Transactional
    void putNonExistingEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        event.setId(longCount.incrementAndGet());

        // Create the Event
        EventDTO eventDTO = eventMapper.toDto(event);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEventMockMvc
            .perform(
                put(ENTITY_API_URL_ID, eventDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(eventDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Event in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        event.setId(longCount.incrementAndGet());

        // Create the Event
        EventDTO eventDTO = eventMapper.toDto(event);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEventMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(eventDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Event in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        event.setId(longCount.incrementAndGet());

        // Create the Event
        EventDTO eventDTO = eventMapper.toDto(event);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEventMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(eventDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Event in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateEventWithPatch() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the event using partial update
        Event partialUpdatedEvent = new Event();
        partialUpdatedEvent.setId(event.getId());

        partialUpdatedEvent.name(UPDATED_NAME).startTime(UPDATED_START_TIME).posterUrl(UPDATED_POSTER_URL);

        restEventMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvent.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvent))
            )
            .andExpect(status().isOk());

        // Validate the Event in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEventUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedEvent, event), getPersistedEvent(event));
    }

    @Test
    @Transactional
    void fullUpdateEventWithPatch() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the event using partial update
        Event partialUpdatedEvent = new Event();
        partialUpdatedEvent.setId(event.getId());

        partialUpdatedEvent
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .startTime(UPDATED_START_TIME)
            .endTime(UPDATED_END_TIME)
            .status(UPDATED_STATUS)
            .posterUrl(UPDATED_POSTER_URL);

        restEventMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvent.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvent))
            )
            .andExpect(status().isOk());

        // Validate the Event in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEventUpdatableFieldsEquals(partialUpdatedEvent, getPersistedEvent(partialUpdatedEvent));
    }

    @Test
    @Transactional
    void patchNonExistingEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        event.setId(longCount.incrementAndGet());

        // Create the Event
        EventDTO eventDTO = eventMapper.toDto(event);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEventMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, eventDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(eventDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Event in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        event.setId(longCount.incrementAndGet());

        // Create the Event
        EventDTO eventDTO = eventMapper.toDto(event);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEventMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(eventDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Event in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        event.setId(longCount.incrementAndGet());

        // Create the Event
        EventDTO eventDTO = eventMapper.toDto(event);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEventMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(eventDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Event in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteEvent() throws Exception {
        // Initialize the database
        insertedEvent = eventRepository.saveAndFlush(event);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the event
        restEventMockMvc
            .perform(delete(ENTITY_API_URL_ID, event.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return eventRepository.count();
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

    protected Event getPersistedEvent(Event event) {
        return eventRepository.findById(event.getId()).orElseThrow();
    }

    protected void assertPersistedEventToMatchAllProperties(Event expectedEvent) {
        assertEventAllPropertiesEquals(expectedEvent, getPersistedEvent(expectedEvent));
    }

    protected void assertPersistedEventToMatchUpdatableProperties(Event expectedEvent) {
        assertEventAllUpdatablePropertiesEquals(expectedEvent, getPersistedEvent(expectedEvent));
    }
}
