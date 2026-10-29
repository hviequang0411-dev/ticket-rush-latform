import React, { useEffect } from 'react';
import { Button, Col, FormText, Row } from 'react-bootstrap';
import { Translate, ValidatedField, ValidatedForm, translate } from 'react-jhipster';
import { Link, useNavigate, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getEntities as getVenues } from 'app/entities/catalog/venue/venue.reducer';
import { EventStatus } from 'app/shared/model/enumerations/event-status.model';
import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';

import { createEntity, getEntity, reset, updateEntity } from './event.reducer';

export const EventUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const venues = useAppSelector(state => state.gateway.venue.entities);
  const eventEntity = useAppSelector(state => state.gateway.event.entity);
  const loading = useAppSelector(state => state.gateway.event.loading);
  const updating = useAppSelector(state => state.gateway.event.updating);
  const updateSuccess = useAppSelector(state => state.gateway.event.updateSuccess);
  const eventStatusValues = Object.keys(EventStatus);

  const handleClose = () => {
    navigate(`/event${location.search}`);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getVenues({}));
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
    values.startTime = convertDateTimeToServer(values.startTime);
    values.endTime = convertDateTimeToServer(values.endTime);

    const entity = {
      ...eventEntity,
      ...values,
      venue: venues.find(it => it.id.toString() === values.venue?.toString()),
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
          startTime: displayDefaultDateTime(),
          endTime: displayDefaultDateTime(),
        }
      : {
          status: 'DRAFT',
          ...eventEntity,
          startTime: convertDateTimeFromServer(eventEntity.startTime),
          endTime: convertDateTimeFromServer(eventEntity.endTime),
          venue: eventEntity?.venue?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="gatewayApp.catalogEvent.home.createOrEditLabel" data-cy="EventCreateUpdateHeading">
            <Translate contentKey="gatewayApp.catalogEvent.home.createOrEditLabel">Create or edit a Event</Translate>
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
                  id="event-id"
                  label={translate('gatewayApp.catalogEvent.id')}
                  validate={{ required: true }}
                />
              )}
              <ValidatedField
                label={translate('gatewayApp.catalogEvent.name')}
                id="event-name"
                name="name"
                data-cy="name"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  maxLength: { value: 200, message: translate('entity.validation.maxlength', { max: 200 }) },
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.catalogEvent.slug')}
                id="event-slug"
                name="slug"
                data-cy="slug"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  maxLength: { value: 220, message: translate('entity.validation.maxlength', { max: 220 }) },
                  pattern: {
                    value: /^[a-z0-9]+(-[a-z0-9]+)*$/,
                    message: translate('entity.validation.pattern', { pattern: '^[a-z0-9]+(-[a-z0-9]+)*$' }),
                  },
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.catalogEvent.description')}
                id="event-description"
                name="description"
                data-cy="description"
                type="textarea"
              />
              <ValidatedField
                label={translate('gatewayApp.catalogEvent.startTime')}
                id="event-startTime"
                name="startTime"
                data-cy="startTime"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('gatewayApp.catalogEvent.endTime')}
                id="event-endTime"
                name="endTime"
                data-cy="endTime"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                label={translate('gatewayApp.catalogEvent.status')}
                id="event-status"
                name="status"
                data-cy="status"
                type="select"
              >
                {eventStatusValues.map(eventStatus => (
                  <option value={eventStatus} key={eventStatus}>
                    {translate(`gatewayApp.EventStatus.${eventStatus}`)}
                  </option>
                ))}
              </ValidatedField>
              <ValidatedField
                label={translate('gatewayApp.catalogEvent.posterUrl')}
                id="event-posterUrl"
                name="posterUrl"
                data-cy="posterUrl"
                type="text"
                validate={{
                  maxLength: { value: 1000, message: translate('entity.validation.maxlength', { max: 1000 }) },
                }}
              />
              <ValidatedField
                id="event-venue"
                name="venue"
                data-cy="venue"
                label={translate('gatewayApp.catalogEvent.venue')}
                type="select"
                required
              >
                <option value="" key="0" />
                {venues
                  ? venues.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.name}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <FormText>
                <Translate contentKey="entity.validation.required">This field is required.</Translate>
              </FormText>
              <Button as={Link as any} id="cancel-save" data-cy="entityCreateCancelButton" to="/event" replace variant="info">
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

export default EventUpdate;
