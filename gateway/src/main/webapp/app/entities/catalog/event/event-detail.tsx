import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './event.reducer';

export const EventDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const eventEntity = useAppSelector(state => state.gateway.event.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="eventDetailsHeading">
          <Translate contentKey="gatewayApp.catalogEvent.detail.title">Event</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="gatewayApp.catalogEvent.id">Id</Translate>
            </span>
          </dt>
          <dd>{eventEntity.id}</dd>
          <dt>
            <span id="name">
              <Translate contentKey="gatewayApp.catalogEvent.name">Name</Translate>
            </span>
          </dt>
          <dd>{eventEntity.name}</dd>
          <dt>
            <span id="slug">
              <Translate contentKey="gatewayApp.catalogEvent.slug">Slug</Translate>
            </span>
          </dt>
          <dd>{eventEntity.slug}</dd>
          <dt>
            <span id="description">
              <Translate contentKey="gatewayApp.catalogEvent.description">Description</Translate>
            </span>
          </dt>
          <dd>{eventEntity.description}</dd>
          <dt>
            <span id="startTime">
              <Translate contentKey="gatewayApp.catalogEvent.startTime">Start Time</Translate>
            </span>
          </dt>
          <dd>{eventEntity.startTime ? <TextFormat value={eventEntity.startTime} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="endTime">
              <Translate contentKey="gatewayApp.catalogEvent.endTime">End Time</Translate>
            </span>
          </dt>
          <dd>{eventEntity.endTime ? <TextFormat value={eventEntity.endTime} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="status">
              <Translate contentKey="gatewayApp.catalogEvent.status">Status</Translate>
            </span>
          </dt>
          <dd>{eventEntity.status}</dd>
          <dt>
            <span id="posterUrl">
              <Translate contentKey="gatewayApp.catalogEvent.posterUrl">Poster Url</Translate>
            </span>
          </dt>
          <dd>{eventEntity.posterUrl}</dd>
          <dt>
            <Translate contentKey="gatewayApp.catalogEvent.venue">Venue</Translate>
          </dt>
          <dd>{eventEntity.venue ? eventEntity.venue.name : ''}</dd>
        </dl>
        <Button as={Link as any} to="/event" replace variant="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button as={Link as any} to={`/event/${eventEntity.id}/edit`} replace variant="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default EventDetail;
