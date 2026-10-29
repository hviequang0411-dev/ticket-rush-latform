import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import OrderItem from './order-item';
import OrderItemDetail from './order-item-detail';

const OrderItemRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<OrderItem />} />
    <Route path=":id">
      <Route index element={<OrderItemDetail />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default OrderItemRoutes;
