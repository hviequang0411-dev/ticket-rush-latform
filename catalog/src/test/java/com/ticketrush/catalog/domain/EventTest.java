package com.ticketrush.catalog.domain;

import static com.ticketrush.catalog.domain.EventTestSamples.*;
import static com.ticketrush.catalog.domain.TicketTypeTestSamples.*;
import static com.ticketrush.catalog.domain.VenueTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.catalog.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Event.class);
        Event event1 = getEventSample1();
        Event event2 = new Event();
        assertThat(event1).isNotEqualTo(event2);

        event2.setId(event1.getId());
        assertThat(event1).isEqualTo(event2);

        event2 = getEventSample2();
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    void ticketTypesTest() {
        Event event = getEventRandomSampleGenerator();
        TicketType ticketTypeBack = getTicketTypeRandomSampleGenerator();

        event.addTicketTypes(ticketTypeBack);
        assertThat(event.getTicketTypeses()).containsOnly(ticketTypeBack);
        assertThat(ticketTypeBack.getEvent()).isEqualTo(event);

        event.removeTicketTypes(ticketTypeBack);
        assertThat(event.getTicketTypeses()).doesNotContain(ticketTypeBack);
        assertThat(ticketTypeBack.getEvent()).isNull();

        event.ticketTypeses(new HashSet<>(Set.of(ticketTypeBack)));
        assertThat(event.getTicketTypeses()).containsOnly(ticketTypeBack);
        assertThat(ticketTypeBack.getEvent()).isEqualTo(event);

        event.setTicketTypeses(new HashSet<>());
        assertThat(event.getTicketTypeses()).doesNotContain(ticketTypeBack);
        assertThat(ticketTypeBack.getEvent()).isNull();
    }

    @Test
    void venueTest() {
        Event event = getEventRandomSampleGenerator();
        Venue venueBack = getVenueRandomSampleGenerator();

        event.setVenue(venueBack);
        assertThat(event.getVenue()).isEqualTo(venueBack);

        event.venue(null);
        assertThat(event.getVenue()).isNull();
    }
}
