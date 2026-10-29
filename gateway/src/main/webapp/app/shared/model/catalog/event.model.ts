import dayjs from 'dayjs';

import { IVenue } from 'app/shared/model/catalog/venue.model';
import { EventStatus } from 'app/shared/model/enumerations/event-status.model';

export interface IEvent {
  id?: number;
  name?: string;
  slug?: string;
  description?: string | null;
  startTime?: dayjs.Dayjs;
  endTime?: dayjs.Dayjs | null;
  status?: keyof typeof EventStatus;
  posterUrl?: string | null;
  venue?: IVenue;
}

export const defaultValue: Readonly<IEvent> = {};
