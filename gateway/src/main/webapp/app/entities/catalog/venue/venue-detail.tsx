import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './venue.reducer';

export const VenueDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const venueEntity = useAppSelector(state => state.gateway.venue.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="venueDetailsHeading">
          <Translate contentKey="gatewayApp.catalogVenue.detail.title">Venue</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="gatewayApp.catalogVenue.id">Id</Translate>
            </span>
          </dt>
          <dd>{venueEntity.id}</dd>
          <dt>
            <span id="name">
              <Translate contentKey="gatewayApp.catalogVenue.name">Name</Translate>
            </span>
          </dt>
          <dd>{venueEntity.name}</dd>
          <dt>
            <span id="address">
              <Translate contentKey="gatewayApp.catalogVenue.address">Address</Translate>
            </span>
          </dt>
          <dd>{venueEntity.address}</dd>
          <dt>
            <span id="city">
              <Translate contentKey="gatewayApp.catalogVenue.city">City</Translate>
            </span>
          </dt>
          <dd>{venueEntity.city}</dd>
          <dt>
            <span id="capacity">
              <Translate contentKey="gatewayApp.catalogVenue.capacity">Capacity</Translate>
            </span>
          </dt>
          <dd>{venueEntity.capacity}</dd>
        </dl>
        <Button as={Link as any} to="/venue" replace variant="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button as={Link as any} to={`/venue/${venueEntity.id}/edit`} replace variant="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default VenueDetail;
