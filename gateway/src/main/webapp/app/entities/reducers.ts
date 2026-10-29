import event from 'app/entities/catalog/event/event.reducer';
import ticketType from 'app/entities/catalog/ticket-type/ticket-type.reducer';
import venue from 'app/entities/catalog/venue/venue.reducer';
import ticketStock from 'app/entities/inventory/ticket-stock/ticket-stock.reducer';
import order from 'app/entities/order/order/order.reducer';
import orderItem from 'app/entities/order/order-item/order-item.reducer';
/* jhipster-needle-add-reducer-import - JHipster will add reducer here */

const entitiesReducers = {
  venue,
  ticketStock,
  event,
  order,
  ticketType,
  orderItem,
  // jhipster-needle-add-reducer-combine - JHipster will add reducer here
};

export default entitiesReducers;
