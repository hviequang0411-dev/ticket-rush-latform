import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { Translate, ValidatedField, ValidatedForm, isNumber, translate } from 'react-jhipster';
import { Link, useNavigate, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { createEntity, getEntity, reset, updateEntity } from './ticket-stock.reducer';

export const TicketStockUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const ticketStockEntity = useAppSelector(state => state.gateway.ticketStock.entity);
  const loading = useAppSelector(state => state.gateway.ticketStock.loading);
  const updating = useAppSelector(state => state.gateway.ticketStock.updating);
  const updateSuccess = useAppSelector(state => state.gateway.ticketStock.updateSuccess);

  const handleClose = () => {
    navigate(`/ticket-stock${location.search}`);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    if (values.id !== undefined && typeof values.id !== 'number') {
      values.id = Number(values.id);
    }
    if (values.ticketTypeId !== undefined && typeof values.ticketTypeId !== 'number') {
      values.ticketTypeId = Number(values.ticketTypeId);
    }
    if (values.eventId !== undefined && typeof values.eventId !== 'number') {
      values.eventId = Number(values.eventId);
    }
    if (values.totalQuantity !== undefined && typeof values.totalQuantity !== 'number') {
      values.totalQuantity = Number(values.totalQuantity);
    }
    if (values.availableQuantity !== undefined && typeof values.availableQuantity !== 'number') {
      values.availableQuantity = Number(values.availableQuantity);
    }

    const entity = {
      ...ticketStockEntity,
      ...values,
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {}
      : {
          ...ticketStockEntity,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="gatewayApp.inventoryTicketStock.home.createOrEditLabel" data-cy="TicketStockCreateUpdateHeading">
            <Translate contentKey="gatewayApp.inventoryTicketStock.home.createOrEditLabel">Create or edit a TicketStock</Translate>
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <p>Loading...</p>
          ) : (
            <ValidatedForm mode="all" defaultValues={defaultValues()} onSubmit={saveEntity}>
              {!isNew && (
                <ValidatedField
                  name="id"
                  required
                  readOnly
                  id="ticket-stock-id"
                  label={translate('gatewayApp.inventoryTicketStock.id')}
                  validate={{ required: true }}
                />
              )}
              <ValidatedField
                label={translate('gatewayApp.inventoryTicketStock.ticketTypeId')}
                id="ticket-stock-ticketTypeId"
                name="ticketTypeId"
                data-cy="ticketTypeId"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  validate: v => isNumber(v) || translate('entity.validation.number'),
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.inventoryTicketStock.eventId')}
                id="ticket-stock-eventId"
                name="eventId"
                data-cy="eventId"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  validate: v => isNumber(v) || translate('entity.validation.number'),
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.inventoryTicketStock.totalQuantity')}
                id="ticket-stock-totalQuantity"
                name="totalQuantity"
                data-cy="totalQuantity"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  min: { value: 0, message: translate('entity.validation.min', { min: 0 }) },
                  validate: v => isNumber(v) || translate('entity.validation.number'),
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.inventoryTicketStock.availableQuantity')}
                id="ticket-stock-availableQuantity"
                name="availableQuantity"
                data-cy="availableQuantity"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  min: { value: 0, message: translate('entity.validation.min', { min: 0 }) },
                  validate: v => isNumber(v) || translate('entity.validation.number'),
                }}
              />
              <Button as={Link as any} id="cancel-save" data-cy="entityCreateCancelButton" to="/ticket-stock" replace variant="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">
                  <Translate contentKey="entity.action.back">Back</Translate>
                </span>
              </Button>
              &nbsp;
              <Button variant="primary" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
                <FontAwesomeIcon icon="save" />
                &nbsp;
                <Translate contentKey="entity.action.save">Save</Translate>
              </Button>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default TicketStockUpdate;
