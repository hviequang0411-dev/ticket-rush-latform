import React from 'react';
import { Route } from 'react-router';

import { ReducersMapObject, combineReducers } from '@reduxjs/toolkit';
import { useStore } from 'react-redux';

import type { InjectableStore } from 'app/config/store';
import PrivateRoute from 'app/shared/auth/private-route';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import PageNotFound from 'app/shared/error/page-not-found';
import { Authority } from 'app/shared/jhipster/constants';

import Event from './catalog/event';
import TicketType from './catalog/ticket-type';
import Venue from './catalog/venue';
import TicketStock from './inventory/ticket-stock';
import Order from './order/order';
import OrderItem from './order/order-item';
import entitiesReducers from './reducers';
/* jhipster-needle-add-route-import - JHipster will add routes here */

export default () => {
  // Use the store provided by the host application, microfrontends must not rely on module singletons.
  const store = useStore() as InjectableStore;
  store.injectReducer('gateway', combineReducers(entitiesReducers as ReducersMapObject));
  return (
    <div>
      <ErrorBoundaryRoutes>
        {/* prettier-ignore */}
        <Route path="/venue/*" element={<Venue />} />
        <Route
          path="/ticket-stock/*"
          element={
            <PrivateRoute hasAnyAuthorities={[Authority.ADMIN]}>
              <TicketStock />
            </PrivateRoute>
          }
        />
        <Route path="/event/*" element={<Event />} />
        <Route
          path="/order/*"
          element={
            <PrivateRoute hasAnyAuthorities={[Authority.ADMIN]}>
              <Order />
            </PrivateRoute>
          }
        />
        <Route path="/ticket-type/*" element={<TicketType />} />
        <Route
          path="/order-item/*"
          element={
            <PrivateRoute hasAnyAuthorities={[Authority.ADMIN]}>
              <OrderItem />
            </PrivateRoute>
          }
        />
        {/* jhipster-needle-add-route-path - JHipster will add routes here */}
        <Route path="*" element={<PageNotFound />} />
      </ErrorBoundaryRoutes>
    </div>
  );
};
