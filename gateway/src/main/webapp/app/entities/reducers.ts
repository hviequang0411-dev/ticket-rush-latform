import event from 'app/entities/catalog/event/event.reducer';
import ticketType from 'app/entities/catalog/ticket-type/ticket-type.reducer';
import venue from 'app/entities/catalog/venue/venue.reducer';
/* jhipster-needle-add-reducer-import - JHipster will add reducer here */

const entitiesReducers = {
  venue,
  event,
  ticketType,
  // jhipster-needle-add-reducer-combine - JHipster will add reducer here
};

export default entitiesReducers;
