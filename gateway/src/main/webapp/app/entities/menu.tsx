import React from 'react';
import { Translate } from 'react-jhipster';

import MenuItem from 'app/shared/layout/menus/menu-item';

const EntitiesMenu = () => {
  return (
    <>
      {/* prettier-ignore */}
      <MenuItem icon="asterisk" to="/venue">
        <Translate contentKey="global.menu.entities.catalogVenue" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/ticket-stock">
        <Translate contentKey="global.menu.entities.inventoryTicketStock" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/event">
        <Translate contentKey="global.menu.entities.catalogEvent" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/order">
        <Translate contentKey="global.menu.entities.orderOrder" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/ticket-type">
        <Translate contentKey="global.menu.entities.catalogTicketType" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/order-item">
        <Translate contentKey="global.menu.entities.orderOrderItem" />
      </MenuItem>
      {/* jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here */}
    </>
  );
};

export default EntitiesMenu;
