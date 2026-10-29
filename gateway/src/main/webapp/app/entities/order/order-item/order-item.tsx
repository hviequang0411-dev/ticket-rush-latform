import React, { useEffect, useState } from 'react';
import { Button, Table } from 'react-bootstrap';
import { Translate, getSortState } from 'react-jhipster';
import { Link, useLocation, useNavigate } from 'react-router';

import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { overrideSortStateWithQueryParams } from 'app/shared/util/entity-utils';
import { ASC, DESC } from 'app/shared/util/pagination.constants';

import { getEntities } from './order-item.reducer';

export const OrderItem = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [sortState, setSortState] = useState(overrideSortStateWithQueryParams(getSortState(pageLocation, 'id'), pageLocation.search));

  const orderItemList = useAppSelector(state => state.gateway.orderItem.entities);
  const loading = useAppSelector(state => state.gateway.orderItem.loading);

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
      <h2 id="order-item-heading" data-cy="OrderItemHeading">
        <Translate contentKey="gatewayApp.orderOrderItem.home.title">Order Items</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" variant="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="gatewayApp.orderOrderItem.home.refreshListLabel">Refresh List</Translate>
          </Button>
        </div>
      </h2>
      <div className="table-responsive">
        {orderItemList?.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('id')}>
                  <Translate contentKey="gatewayApp.orderOrderItem.id">Id</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
                </th>
                <th className="hand" onClick={sort('eventId')}>
                  <Translate contentKey="gatewayApp.orderOrderItem.eventId">Event Id</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('eventId')} />
                </th>
                <th className="hand" onClick={sort('ticketTypeId')}>
                  <Translate contentKey="gatewayApp.orderOrderItem.ticketTypeId">Ticket Type Id</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('ticketTypeId')} />
                </th>
                <th className="hand" onClick={sort('quantity')}>
                  <Translate contentKey="gatewayApp.orderOrderItem.quantity">Quantity</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('quantity')} />
                </th>
                <th className="hand" onClick={sort('unitPrice')}>
                  <Translate contentKey="gatewayApp.orderOrderItem.unitPrice">Unit Price</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('unitPrice')} />
                </th>
                <th>
                  <Translate contentKey="gatewayApp.orderOrderItem.order">Order</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {orderItemList.map(orderItem => (
                <tr key={`entity-${orderItem.id}`} data-cy="entityTable">
                  <td>
                    <Button as={Link as any} to={`/order-item/${orderItem.id}`} variant="link" size="sm">
                      {orderItem.id}
                    </Button>
                  </td>
                  <td>{orderItem.eventId}</td>
                  <td>{orderItem.ticketTypeId}</td>
                  <td>{orderItem.quantity}</td>
                  <td>{orderItem.unitPrice}</td>
                  <td>{orderItem.order ? <Link to={`/order/${orderItem.order.id}`}>{orderItem.order.id}</Link> : ''}</td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button as={Link as any} to={`/order-item/${orderItem.id}`} variant="info" size="sm" data-cy="entityDetailsButton">
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
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
              <Translate contentKey="gatewayApp.orderOrderItem.home.notFound">No Order Items found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default OrderItem;
