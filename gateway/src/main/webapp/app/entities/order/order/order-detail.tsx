import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './order.reducer';

export const OrderDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const orderEntity = useAppSelector(state => state.gateway.order.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="orderDetailsHeading">
          <Translate contentKey="gatewayApp.orderOrder.detail.title">Order</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="gatewayApp.orderOrder.id">Id</Translate>
            </span>
          </dt>
          <dd>{orderEntity.id}</dd>
          <dt>
            <span id="code">
              <Translate contentKey="gatewayApp.orderOrder.code">Code</Translate>
            </span>
          </dt>
          <dd>{orderEntity.code}</dd>
          <dt>
            <span id="userLogin">
              <Translate contentKey="gatewayApp.orderOrder.userLogin">User Login</Translate>
            </span>
          </dt>
          <dd>{orderEntity.userLogin}</dd>
          <dt>
            <span id="status">
              <Translate contentKey="gatewayApp.orderOrder.status">Status</Translate>
            </span>
          </dt>
          <dd>{orderEntity.status}</dd>
          <dt>
            <span id="totalAmount">
              <Translate contentKey="gatewayApp.orderOrder.totalAmount">Total Amount</Translate>
            </span>
          </dt>
          <dd>{orderEntity.totalAmount}</dd>
          <dt>
            <span id="currency">
              <Translate contentKey="gatewayApp.orderOrder.currency">Currency</Translate>
            </span>
          </dt>
          <dd>{orderEntity.currency}</dd>
          <dt>
            <span id="idempotencyKey">
              <Translate contentKey="gatewayApp.orderOrder.idempotencyKey">Idempotency Key</Translate>
            </span>
          </dt>
          <dd>{orderEntity.idempotencyKey}</dd>
          <dt>
            <span id="failureReason">
              <Translate contentKey="gatewayApp.orderOrder.failureReason">Failure Reason</Translate>
            </span>
          </dt>
          <dd>{orderEntity.failureReason}</dd>
          <dt>
            <span id="expiresAt">
              <Translate contentKey="gatewayApp.orderOrder.expiresAt">Expires At</Translate>
            </span>
          </dt>
          <dd>{orderEntity.expiresAt ? <TextFormat value={orderEntity.expiresAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="createdAt">
              <Translate contentKey="gatewayApp.orderOrder.createdAt">Created At</Translate>
            </span>
          </dt>
          <dd>{orderEntity.createdAt ? <TextFormat value={orderEntity.createdAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="updatedAt">
              <Translate contentKey="gatewayApp.orderOrder.updatedAt">Updated At</Translate>
            </span>
          </dt>
          <dd>{orderEntity.updatedAt ? <TextFormat value={orderEntity.updatedAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
        </dl>
        <Button as={Link as any} to="/order" replace variant="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button as={Link as any} to={`/order/${orderEntity.id}/edit`} replace variant="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default OrderDetail;
