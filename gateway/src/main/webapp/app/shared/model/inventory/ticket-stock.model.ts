export interface ITicketStock {
  id?: number;
  ticketTypeId?: number;
  eventId?: number;
  totalQuantity?: number;
  availableQuantity?: number;
}

export const defaultValue: Readonly<ITicketStock> = {};
