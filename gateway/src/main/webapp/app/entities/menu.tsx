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
      <MenuItem icon="asterisk" to="/event">
        <Translate contentKey="global.menu.entities.catalogEvent" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/ticket-type">
        <Translate contentKey="global.menu.entities.catalogTicketType" />
      </MenuItem>
      {/* jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here */}
    </>
  );
};

export default EntitiesMenu;
