import React, { useEffect, useState } from 'react';
import { Button, Table } from 'react-bootstrap';
import { TextFormat, Translate, getSortState } from 'react-jhipster';
import { Link, useLocation, useNavigate } from 'react-router';

import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { overrideSortStateWithQueryParams } from 'app/shared/util/entity-utils';
import { ASC, DESC } from 'app/shared/util/pagination.constants';

import { getEntities } from './ticket-type.reducer';

export const TicketType = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [sortState, setSortState] = useState(overrideSortStateWithQueryParams(getSortState(pageLocation, 'id'), pageLocation.search));

  const ticketTypeList = useAppSelector(state => state.gateway.ticketType.entities);
  const loading = useAppSelector(state => state.gateway.ticketType.loading);

  const getAllEntities = () => {
    dispatch(
      getEntities({
        sort: `${sortState.sort},${sortState.order}`,
      }),
    );
  };

  const sortEntities = () => {
    getAllEntities();
    const endURL = `?sort=${sortState.sort},${sortState.order}`;
    if (pageLocation.search !== endURL) {
      navigate(`${pageLocation.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [sortState.order, sortState.sort]);

  const sort = p => () => {
    setSortState({
      ...sortState,
      order: sortState.order === ASC ? DESC : ASC,
      sort: p,
    });
  };

  const handleSyncList = () => {
    sortEntities();
  };

  const getSortIconByFieldName = (fieldName: string) => {
    const sortFieldName = sortState.sort;
    const { order } = sortState;
    if (sortFieldName !== fieldName) {
      return faSort;
    }
    return order === ASC ? faSortUp : faSortDown;
  };

  return (
    <div>
      <h2 id="ticket-type-heading" data-cy="TicketTypeHeading">
        <Translate contentKey="gatewayApp.catalogTicketType.home.title">Ticket Types</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" variant="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="gatewayApp.catalogTicketType.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link to="/ticket-type/new" className="btn btn-primary jh-create-entity" id="jh-create-entity" data-cy="entityCreateButton">
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="gatewayApp.catalogTicketType.home.createLabel">Create new Ticket Type</Translate>
          </Link>
        </div>
      </h2>
      <div className="table-responsive">
        {ticketTypeList?.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('id')}>
                  <Translate contentKey="gatewayApp.catalogTicketType.id">Id</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
                </th>
                <th className="hand" onClick={sort('name')}>
                  <Translate contentKey="gatewayApp.catalogTicketType.name">Name</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('name')} />
                </th>
                <th className="hand" onClick={sort('price')}>
                  <Translate contentKey="gatewayApp.catalogTicketType.price">Price</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('price')} />
                </th>
                <th className="hand" onClick={sort('currency')}>
                  <Translate contentKey="gatewayApp.catalogTicketType.currency">Currency</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('currency')} />
                </th>
                <th className="hand" onClick={sort('totalQuantity')}>
                  <Translate contentKey="gatewayApp.catalogTicketType.totalQuantity">Total Quantity</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('totalQuantity')} />
                </th>
                <th className="hand" onClick={sort('saleStart')}>
                  <Translate contentKey="gatewayApp.catalogTicketType.saleStart">Sale Start</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('saleStart')} />
                </th>
                <th className="hand" onClick={sort('saleEnd')}>
                  <Translate contentKey="gatewayApp.catalogTicketType.saleEnd">Sale End</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('saleEnd')} />
                </th>
                <th>
                  <Translate contentKey="gatewayApp.catalogTicketType.event">Event</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {ticketTypeList.map(ticketType => (
                <tr key={`entity-${ticketType.id}`} data-cy="entityTable">
                  <td>
                    <Button as={Link as any} to={`/ticket-type/${ticketType.id}`} variant="link" size="sm">
                      {ticketType.id}
                    </Button>
                  </td>
                  <td>{ticketType.name}</td>
                  <td>{ticketType.price}</td>
                  <td>{ticketType.currency}</td>
                  <td>{ticketType.totalQuantity}</td>
                  <td>{ticketType.saleStart ? <TextFormat type="date" value={ticketType.saleStart} format={APP_DATE_FORMAT} /> : null}</td>
                  <td>{ticketType.saleEnd ? <TextFormat type="date" value={ticketType.saleEnd} format={APP_DATE_FORMAT} /> : null}</td>
                  <td>{ticketType.event ? <Link to={`/event/${ticketType.event.id}`}>{ticketType.event.name}</Link> : ''}</td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button as={Link as any} to={`/ticket-type/${ticketType.id}`} variant="info" size="sm" data-cy="entityDetailsButton">
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                      <Button
                        as={Link as any}
                        to={`/ticket-type/${ticketType.id}/edit`}
                        variant="primary"
                        size="sm"
                        data-cy="entityEditButton"
                      >
                        <FontAwesomeIcon icon="pencil-alt" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.edit">Edit</Translate>
                        </span>
                      </Button>
                      <Button
                        onClick={() => (globalThis.location.href = `/ticket-type/${ticketType.id}/delete`)}
                        variant="danger"
                        size="sm"
                        data-cy="entityDeleteButton"
                      >
                        <FontAwesomeIcon icon="trash" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.delete">Delete</Translate>
                        </span>
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !loading && (
            <div className="alert alert-warning">
              <Translate contentKey="gatewayApp.catalogTicketType.home.notFound">No Ticket Types found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default TicketType;
