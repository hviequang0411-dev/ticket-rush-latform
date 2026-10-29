import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import TicketType from './ticket-type';
import TicketTypeDeleteDialog from './ticket-type-delete-dialog';
import TicketTypeDetail from './ticket-type-detail';
import TicketTypeUpdate from './ticket-type-update';

const TicketTypeRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<TicketType />} />
    <Route path="new" element={<TicketTypeUpdate />} />
    <Route path=":id">
      <Route index element={<TicketTypeDetail />} />
      <Route path="edit" element={<TicketTypeUpdate />} />
      <Route path="delete" element={<TicketTypeDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default TicketTypeRoutes;
