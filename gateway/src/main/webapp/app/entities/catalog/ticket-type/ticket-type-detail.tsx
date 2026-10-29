import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './ticket-type.reducer';

export const TicketTypeDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const ticketTypeEntity = useAppSelector(state => state.gateway.ticketType.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="ticketTypeDetailsHeading">
          <Translate contentKey="gatewayApp.catalogTicketType.detail.title">TicketType</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="gatewayApp.catalogTicketType.id">Id</Translate>
            </span>
          </dt>
          <dd>{ticketTypeEntity.id}</dd>
          <dt>
            <span id="name">
              <Translate contentKey="gatewayApp.catalogTicketType.name">Name</Translate>
            </span>
          </dt>
          <dd>{ticketTypeEntity.name}</dd>
          <dt>
            <span id="price">
              <Translate contentKey="gatewayApp.catalogTicketType.price">Price</Translate>
            </span>
          </dt>
          <dd>{ticketTypeEntity.price}</dd>
          <dt>
            <span id="currency">
              <Translate contentKey="gatewayApp.catalogTicketType.currency">Currency</Translate>
            </span>
          </dt>
          <dd>{ticketTypeEntity.currency}</dd>
          <dt>
            <span id="totalQuantity">
              <Translate contentKey="gatewayApp.catalogTicketType.totalQuantity">Total Quantity</Translate>
            </span>
          </dt>
          <dd>{ticketTypeEntity.totalQuantity}</dd>
          <dt>
            <span id="saleStart">
              <Translate contentKey="gatewayApp.catalogTicketType.saleStart">Sale Start</Translate>
            </span>
          </dt>
          <dd>
            {ticketTypeEntity.saleStart ? <TextFormat value={ticketTypeEntity.saleStart} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <span id="saleEnd">
              <Translate contentKey="gatewayApp.catalogTicketType.saleEnd">Sale End</Translate>
            </span>
          </dt>
          <dd>{ticketTypeEntity.saleEnd ? <TextFormat value={ticketTypeEntity.saleEnd} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <Translate contentKey="gatewayApp.catalogTicketType.event">Event</Translate>
          </dt>
          <dd>{ticketTypeEntity.event ? ticketTypeEntity.event.name : ''}</dd>
        </dl>
        <Button as={Link as any} to="/ticket-type" replace variant="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button as={Link as any} to={`/ticket-type/${ticketTypeEntity.id}/edit`} replace variant="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default TicketTypeDetail;
