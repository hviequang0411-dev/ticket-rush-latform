import { useCallback, useEffect, useState } from 'react';
import { AdminApi, type AdminEvent } from '../../api/adminApi';
import type { ApiErrorProblem } from '../../types/models';

/** Every event with its stock, plus lookups by event and ticket type id, refreshed every {@code refreshMs} if set. */
export function useAdminEvents(refreshMs?: number) {
  const [events, setEvents] = useState<AdminEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiErrorProblem | null>(null);

  const reload = useCallback(async () => {
    try {
      setEvents(await AdminApi.events());
      setError(null);
    } catch (e: unknown) {
      setError(e as ApiErrorProblem);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
    if (!refreshMs) return;
    const timer = setInterval(reload, refreshMs);
    return () => clearInterval(timer);
  }, [reload, refreshMs]);

  const eventName = (id: number) => events.find(event => event.id === id)?.name ?? `Sự kiện #${id}`;
  const ticketTypeName = (id: number) =>
    events.flatMap(event => event.ticketTypes).find(type => type.id === id)?.name ?? `Loại vé #${id}`;

  return { events, loading, error, reload, eventName, ticketTypeName };
}
