package com.ticketrush.catalog.service;

import com.ticketrush.catalog.service.dto.VenueDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.ticketrush.catalog.domain.Venue}.
 */
public interface VenueService {
    /**
     * Save a venue.
     *
     * @param venueDTO the entity to save.
     * @return the persisted entity.
     */
    VenueDTO save(VenueDTO venueDTO);

    /**
     * Updates a venue.
     *
     * @param venueDTO the entity to update.
     * @return the persisted entity.
     */
    VenueDTO update(VenueDTO venueDTO);

    /**
     * Partially updates a venue.
     *
     * @param venueDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<VenueDTO> partialUpdate(VenueDTO venueDTO);

    /**
     * Get all the venues.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<VenueDTO> findAll(Pageable pageable);

    /**
     * Get the "id" venue.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<VenueDTO> findOne(Long id);

    /**
     * Delete the "id" venue.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
