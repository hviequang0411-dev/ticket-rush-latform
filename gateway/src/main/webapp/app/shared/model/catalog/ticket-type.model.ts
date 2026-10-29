import dayjs from 'dayjs';

import { IEvent } from 'app/shared/model/catalog/event.model';

export interface ITicketType {
  id?: number;
  name?: string;
  price?: number;
  currency?: string;
  totalQuantity?: number;
  saleStart?: dayjs.Dayjs | null;
  saleEnd?: dayjs.Dayjs | null;
  event?: IEvent;
}

export const defaultValue: Readonly<ITicketType> = {};
