import dayjs from 'dayjs';

import { PaymentProvider } from 'app/shared/model/enumerations/payment-provider.model';
import { PaymentStatus } from 'app/shared/model/enumerations/payment-status.model';

export interface IPayment {
  id?: number;
  orderId?: number;
  orderCode?: string;
  amount?: number;
  currency?: string;
  provider?: keyof typeof PaymentProvider;
  status?: keyof typeof PaymentStatus;
  providerRef?: string;
  providerTransactionId?: string | null;
  paymentUrl?: string | null;
  failureReason?: string | null;
  expiresAt?: dayjs.Dayjs;
  createdAt?: dayjs.Dayjs;
  updatedAt?: dayjs.Dayjs | null;
}

export const defaultValue: Readonly<IPayment> = {};
