import React from 'react';
import { Translate } from 'react-jhipster';

import { useAppSelector } from 'app/config/store';
import { hasAnyAuthority } from 'app/shared/auth/private-route';
import { Authority } from 'app/shared/jhipster/constants';

import MenuItem from 'app/shared/layout/menus/menu-item';

const EntitiesMenu = () => {
  // Stock and orders are operations screens; their APIs only answer admins
  const isAdmin = useAppSelector(state => hasAnyAuthority(state.authentication.account.authorities, [Authority.ADMIN]));
  return (
    <>
      {/* prettier-ignore */}
      <MenuItem icon="asterisk" to="/venue">
        <Translate contentKey="global.menu.entities.catalogVenue" />
      </MenuItem>
      {isAdmin && (
        <MenuItem icon="asterisk" to="/ticket-stock">
          <Translate contentKey="global.menu.entities.inventoryTicketStock" />
        </MenuItem>
      )}
      <MenuItem icon="asterisk" to="/event">
        <Translate contentKey="global.menu.entities.catalogEvent" />
      </MenuItem>
      {isAdmin && (
        <MenuItem icon="asterisk" to="/order">
          <Translate contentKey="global.menu.entities.orderOrder" />
        </MenuItem>
      )}
      <MenuItem icon="asterisk" to="/ticket-type">
        <Translate contentKey="global.menu.entities.catalogTicketType" />
      </MenuItem>
      {isAdmin && (
        <MenuItem icon="asterisk" to="/order-item">
          <Translate contentKey="global.menu.entities.orderOrderItem" />
        </MenuItem>
      )}
      {isAdmin && (
        <MenuItem icon="asterisk" to="/payment">
          <Translate contentKey="global.menu.entities.paymentPayment" />
        </MenuItem>
      )}
      {/* jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here */}
    </>
  );
};

export default EntitiesMenu;
