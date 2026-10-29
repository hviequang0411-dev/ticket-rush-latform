export interface IVenue {
  id?: number;
  name?: string;
  address?: string;
  city?: string;
  capacity?: number;
}

export const defaultValue: Readonly<IVenue> = {};
