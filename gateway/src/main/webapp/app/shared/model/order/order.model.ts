import dayjs from 'dayjs';

import { OrderStatus } from 'app/shared/model/enumerations/order-status.model';

export interface IOrder {
  id?: number;
  code?: string;
  userLogin?: string;
  status?: keyof typeof OrderStatus;
  totalAmount?: number;
  currency?: string;
  idempotencyKey?: string;
  failureReason?: string | null;
  expiresAt?: dayjs.Dayjs | null;
  createdAt?: dayjs.Dayjs;
  updatedAt?: dayjs.Dayjs | null;
}

export const defaultValue: Readonly<IOrder> = {};
