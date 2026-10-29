import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import TicketStock from './ticket-stock';
import TicketStockDeleteDialog from './ticket-stock-delete-dialog';
import TicketStockDetail from './ticket-stock-detail';
import TicketStockUpdate from './ticket-stock-update';

const TicketStockRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<TicketStock />} />
    <Route path="new" element={<TicketStockUpdate />} />
    <Route path=":id">
      <Route index element={<TicketStockDetail />} />
      <Route path="edit" element={<TicketStockUpdate />} />
      <Route path="delete" element={<TicketStockDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default TicketStockRoutes;
