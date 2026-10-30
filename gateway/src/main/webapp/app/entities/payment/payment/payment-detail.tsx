import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './payment.reducer';

export const PaymentDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const paymentEntity = useAppSelector(state => state.gateway.payment.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="paymentDetailsHeading">
          <Translate contentKey="gatewayApp.paymentPayment.detail.title">Payment</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="gatewayApp.paymentPayment.id">Id</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.id}</dd>
          <dt>
            <span id="orderId">
              <Translate contentKey="gatewayApp.paymentPayment.orderId">Order Id</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.orderId}</dd>
          <dt>
            <span id="orderCode">
              <Translate contentKey="gatewayApp.paymentPayment.orderCode">Order Code</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.orderCode}</dd>
          <dt>
            <span id="amount">
              <Translate contentKey="gatewayApp.paymentPayment.amount">Amount</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.amount}</dd>
          <dt>
            <span id="currency">
              <Translate contentKey="gatewayApp.paymentPayment.currency">Currency</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.currency}</dd>
          <dt>
            <span id="provider">
              <Translate contentKey="gatewayApp.paymentPayment.provider">Provider</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.provider}</dd>
          <dt>
            <span id="status">
              <Translate contentKey="gatewayApp.paymentPayment.status">Status</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.status}</dd>
          <dt>
            <span id="providerRef">
              <Translate contentKey="gatewayApp.paymentPayment.providerRef">Provider Ref</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.providerRef}</dd>
          <dt>
            <span id="providerTransactionId">
              <Translate contentKey="gatewayApp.paymentPayment.providerTransactionId">Provider Transaction Id</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.providerTransactionId}</dd>
          <dt>
            <span id="paymentUrl">
              <Translate contentKey="gatewayApp.paymentPayment.paymentUrl">Payment Url</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.paymentUrl}</dd>
          <dt>
            <span id="failureReason">
              <Translate contentKey="gatewayApp.paymentPayment.failureReason">Failure Reason</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.failureReason}</dd>
          <dt>
            <span id="expiresAt">
              <Translate contentKey="gatewayApp.paymentPayment.expiresAt">Expires At</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.expiresAt ? <TextFormat value={paymentEntity.expiresAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="createdAt">
              <Translate contentKey="gatewayApp.paymentPayment.createdAt">Created At</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.createdAt ? <TextFormat value={paymentEntity.createdAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="updatedAt">
              <Translate contentKey="gatewayApp.paymentPayment.updatedAt">Updated At</Translate>
            </span>
          </dt>
          <dd>{paymentEntity.updatedAt ? <TextFormat value={paymentEntity.updatedAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
        </dl>
        <Button as={Link as any} to="/payment" replace variant="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default PaymentDetail;
