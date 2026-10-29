import React, { useEffect } from 'react';
import { Button, Col, FormText, Row } from 'react-bootstrap';
import { Translate, ValidatedField, ValidatedForm, isNumber, translate } from 'react-jhipster';
import { Link, useNavigate, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getEntities as getEvents } from 'app/entities/catalog/event/event.reducer';
import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';

import { createEntity, getEntity, reset, updateEntity } from './ticket-type.reducer';

export const TicketTypeUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const events = useAppSelector(state => state.gateway.event.entities);
  const ticketTypeEntity = useAppSelector(state => state.gateway.ticketType.entity);
  const loading = useAppSelector(state => state.gateway.ticketType.loading);
  const updating = useAppSelector(state => state.gateway.ticketType.updating);
  const updateSuccess = useAppSelector(state => state.gateway.ticketType.updateSuccess);

  const handleClose = () => {
    navigate('/ticket-type');
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getEvents({}));
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
    if (values.price !== undefined && typeof values.price !== 'number') {
      values.price = Number(values.price);
    }
    if (values.totalQuantity !== undefined && typeof values.totalQuantity !== 'number') {
      values.totalQuantity = Number(values.totalQuantity);
    }
    values.saleStart = convertDateTimeToServer(values.saleStart);
    values.saleEnd = convertDateTimeToServer(values.saleEnd);

    const entity = {
      ...ticketTypeEntity,
      ...values,
      event: events.find(it => it.id.toString() === values.event?.toString()),
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {
          saleStart: displayDefaultDateTime(),
          saleEnd: displayDefaultDateTime(),
        }
      : {
          ...ticketTypeEntity,
          saleStart: convertDateTimeFromServer(ticketTypeEntity.saleStart),
          saleEnd: convertDateTimeFromServer(ticketTypeEntity.saleEnd),
          event: ticketTypeEntity?.event?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="gatewayApp.catalogTicketType.home.createOrEditLabel" data-cy="TicketTypeCreateUpdateHeading">
            <Translate contentKey="gatewayApp.catalogTicketType.home.createOrEditLabel">Create or edit a TicketType</Translate>
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
                  id="ticket-type-id"
                  label={translate('gatewayApp.catalogTicketType.id')}
                  validate={{ required: true }}
                />
              )}
              <ValidatedField
                label={translate('gatewayApp.catalogTicketType.name')}
                id="ticket-type-name"
                name="name"
                data-cy="name"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  maxLength: { value: 100, message: translate('entity.validation.maxlength', { max: 100 }) },
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.catalogTicketType.price')}
                id="ticket-type-price"
                name="price"
                data-cy="price"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  min: { value: 0, message: translate('entity.validation.min', { min: 0 }) },
                  validate: v => isNumber(v) || translate('entity.validation.number'),
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.catalogTicketType.currency')}
                id="ticket-type-currency"
                name="currency"
                data-cy="currency"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  minLength: { value: 3, message: translate('entity.validation.minlength', { min: 3 }) },
                  maxLength: { value: 3, message: translate('entity.validation.maxlength', { max: 3 }) },
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.catalogTicketType.totalQuantity')}
                id="ticket-type-totalQuantity"
                name="totalQuantity"
                data-cy="totalQuantity"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  min: { value: 1, message: translate('entity.validation.min', { min: 1 }) },
                  validate: v => isNumber(v) || translate('entity.validation.number'),
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.catalogTicketType.saleStart')}
                id="ticket-type-saleStart"
                name="saleStart"
                data-cy="saleStart"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                label={translate('gatewayApp.catalogTicketType.saleEnd')}
                id="ticket-type-saleEnd"
                name="saleEnd"
                data-cy="saleEnd"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                id="ticket-type-event"
                name="event"
                data-cy="event"
                label={translate('gatewayApp.catalogTicketType.event')}
                type="select"
                required
              >
                <option value="" key="0" />
                {events
                  ? events.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.name}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <FormText>
                <Translate contentKey="entity.validation.required">This field is required.</Translate>
              </FormText>
              <Button as={Link as any} id="cancel-save" data-cy="entityCreateCancelButton" to="/ticket-type" replace variant="info">
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

export default TicketTypeUpdate;
