import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import Venue from './venue';
import VenueDeleteDialog from './venue-delete-dialog';
import VenueDetail from './venue-detail';
import VenueUpdate from './venue-update';

const VenueRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<Venue />} />
    <Route path="new" element={<VenueUpdate />} />
    <Route path=":id">
      <Route index element={<VenueDetail />} />
      <Route path="edit" element={<VenueUpdate />} />
      <Route path="delete" element={<VenueDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default VenueRoutes;
