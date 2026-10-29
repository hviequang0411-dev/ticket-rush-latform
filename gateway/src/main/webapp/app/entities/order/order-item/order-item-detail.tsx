import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './order-item.reducer';

export const OrderItemDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const orderItemEntity = useAppSelector(state => state.gateway.orderItem.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="orderItemDetailsHeading">
          <Translate contentKey="gatewayApp.orderOrderItem.detail.title">OrderItem</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="gatewayApp.orderOrderItem.id">Id</Translate>
            </span>
          </dt>
          <dd>{orderItemEntity.id}</dd>
          <dt>
            <span id="eventId">
              <Translate contentKey="gatewayApp.orderOrderItem.eventId">Event Id</Translate>
            </span>
          </dt>
          <dd>{orderItemEntity.eventId}</dd>
          <dt>
            <span id="ticketTypeId">
              <Translate contentKey="gatewayApp.orderOrderItem.ticketTypeId">Ticket Type Id</Translate>
            </span>
          </dt>
          <dd>{orderItemEntity.ticketTypeId}</dd>
          <dt>
            <span id="quantity">
              <Translate contentKey="gatewayApp.orderOrderItem.quantity">Quantity</Translate>
            </span>
          </dt>
          <dd>{orderItemEntity.quantity}</dd>
          <dt>
            <span id="unitPrice">
              <Translate contentKey="gatewayApp.orderOrderItem.unitPrice">Unit Price</Translate>
            </span>
          </dt>
          <dd>{orderItemEntity.unitPrice}</dd>
          <dt>
            <Translate contentKey="gatewayApp.orderOrderItem.order">Order</Translate>
          </dt>
          <dd>{orderItemEntity.order ? orderItemEntity.order.id : ''}</dd>
        </dl>
        <Button as={Link as any} to="/order-item" replace variant="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button as={Link as any} to={`/order-item/${orderItemEntity.id}/edit`} replace variant="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default OrderItemDetail;
