import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './ticket-stock.reducer';

export const TicketStockDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const ticketStockEntity = useAppSelector(state => state.gateway.ticketStock.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="ticketStockDetailsHeading">
          <Translate contentKey="gatewayApp.inventoryTicketStock.detail.title">TicketStock</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="gatewayApp.inventoryTicketStock.id">Id</Translate>
            </span>
          </dt>
          <dd>{ticketStockEntity.id}</dd>
          <dt>
            <span id="ticketTypeId">
              <Translate contentKey="gatewayApp.inventoryTicketStock.ticketTypeId">Ticket Type Id</Translate>
            </span>
          </dt>
          <dd>{ticketStockEntity.ticketTypeId}</dd>
          <dt>
            <span id="eventId">
              <Translate contentKey="gatewayApp.inventoryTicketStock.eventId">Event Id</Translate>
            </span>
          </dt>
          <dd>{ticketStockEntity.eventId}</dd>
          <dt>
            <span id="totalQuantity">
              <Translate contentKey="gatewayApp.inventoryTicketStock.totalQuantity">Total Quantity</Translate>
            </span>
          </dt>
          <dd>{ticketStockEntity.totalQuantity}</dd>
          <dt>
            <span id="availableQuantity">
              <Translate contentKey="gatewayApp.inventoryTicketStock.availableQuantity">Available Quantity</Translate>
            </span>
          </dt>
          <dd>{ticketStockEntity.availableQuantity}</dd>
        </dl>
        <Button as={Link as any} to="/ticket-stock" replace variant="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button as={Link as any} to={`/ticket-stock/${ticketStockEntity.id}/edit`} replace variant="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default TicketStockDetail;
