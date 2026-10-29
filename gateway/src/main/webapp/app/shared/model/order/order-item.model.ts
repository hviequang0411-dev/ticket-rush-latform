import { IOrder } from 'app/shared/model/order/order.model';

export interface IOrderItem {
  id?: number;
  eventId?: number;
  ticketTypeId?: number;
  quantity?: number;
  unitPrice?: number;
  order?: IOrder;
}

export const defaultValue: Readonly<IOrderItem> = {};
