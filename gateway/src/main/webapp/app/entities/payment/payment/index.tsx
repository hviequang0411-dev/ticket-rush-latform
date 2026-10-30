import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import Payment from './payment';
import PaymentDetail from './payment-detail';

const PaymentRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<Payment />} />
    <Route path=":id">
      <Route index element={<PaymentDetail />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default PaymentRoutes;
