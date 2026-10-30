import React, { useEffect, useState } from 'react';
import { Button, Table } from 'react-bootstrap';
import { JhiItemCount, JhiPagination, TextFormat, Translate, getPaginationState } from 'react-jhipster';
import { Link, useLocation, useNavigate } from 'react-router';

import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { overridePaginationStateWithQueryParams } from 'app/shared/util/entity-utils';
import { ASC, DESC, ITEMS_PER_PAGE, SORT } from 'app/shared/util/pagination.constants';

import { getEntities } from './payment.reducer';

export const Payment = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [paginationState, setPaginationState] = useState(
    overridePaginationStateWithQueryParams(getPaginationState(pageLocation, ITEMS_PER_PAGE, 'id'), pageLocation.search),
  );

  const paymentList = useAppSelector(state => state.gateway.payment.entities);
  const loading = useAppSelector(state => state.gateway.payment.loading);
  const totalItems = useAppSelector(state => state.gateway.payment.totalItems);

  const getAllEntities = () => {
    dispatch(
      getEntities({
        page: paginationState.activePage - 1,
        size: paginationState.itemsPerPage,
        sort: `${paginationState.sort},${paginationState.order}`,
      }),
    );
  };

  const sortEntities = () => {
    getAllEntities();
    const endURL = `?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`;
    if (pageLocation.search !== endURL) {
      navigate(`${pageLocation.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [paginationState.activePage, paginationState.order, paginationState.sort]);

  useEffect(() => {
    const params = new URLSearchParams(pageLocation.search);
    const page = params.get('page');
    const sort = params.get(SORT);
    if (page && sort) {
      const sortSplit = sort.split(',');
      setPaginationState({
        ...paginationState,
        activePage: +page,
        sort: sortSplit[0],
        order: sortSplit[1],
      });
    }
  }, [pageLocation.search]);

  const sort = p => () => {
    setPaginationState({
      ...paginationState,
      order: paginationState.order === ASC ? DESC : ASC,
      sort: p,
    });
  };

  const handlePagination = currentPage =>
    setPaginationState({
      ...paginationState,
      activePage: currentPage,
    });

  const handleSyncList = () => {
    sortEntities();
  };

  const getSortIconByFieldName = (fieldName: string) => {
    const sortFieldName = paginationState.sort;
    const { order } = paginationState;
    if (sortFieldName !== fieldName) {
      return faSort;
    }
    return order === ASC ? faSortUp : faSortDown;
  };

  return (
    <div>
      <h2 id="payment-heading" data-cy="PaymentHeading">
        <Translate contentKey="gatewayApp.paymentPayment.home.title">Payments</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" variant="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="gatewayApp.paymentPayment.home.refreshListLabel">Refresh List</Translate>
          </Button>
        </div>
      </h2>
      <div className="table-responsive">
        {paymentList?.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('id')}>
                  <Translate contentKey="gatewayApp.paymentPayment.id">Id</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
                </th>
                <th className="hand" onClick={sort('orderId')}>
                  <Translate contentKey="gatewayApp.paymentPayment.orderId">Order Id</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('orderId')} />
                </th>
                <th className="hand" onClick={sort('orderCode')}>
                  <Translate contentKey="gatewayApp.paymentPayment.orderCode">Order Code</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('orderCode')} />
                </th>
                <th className="hand" onClick={sort('amount')}>
                  <Translate contentKey="gatewayApp.paymentPayment.amount">Amount</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('amount')} />
                </th>
                <th className="hand" onClick={sort('currency')}>
                  <Translate contentKey="gatewayApp.paymentPayment.currency">Currency</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('currency')} />
                </th>
                <th className="hand" onClick={sort('provider')}>
                  <Translate contentKey="gatewayApp.paymentPayment.provider">Provider</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('provider')} />
                </th>
                <th className="hand" onClick={sort('status')}>
                  <Translate contentKey="gatewayApp.paymentPayment.status">Status</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('status')} />
                </th>
                <th className="hand" onClick={sort('providerRef')}>
                  <Translate contentKey="gatewayApp.paymentPayment.providerRef">Provider Ref</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('providerRef')} />
                </th>
                <th className="hand" onClick={sort('providerTransactionId')}>
                  <Translate contentKey="gatewayApp.paymentPayment.providerTransactionId">Provider Transaction Id</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('providerTransactionId')} />
                </th>
                <th className="hand" onClick={sort('paymentUrl')}>
                  <Translate contentKey="gatewayApp.paymentPayment.paymentUrl">Payment Url</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('paymentUrl')} />
                </th>
                <th className="hand" onClick={sort('failureReason')}>
                  <Translate contentKey="gatewayApp.paymentPayment.failureReason">Failure Reason</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('failureReason')} />
                </th>
                <th className="hand" onClick={sort('expiresAt')}>
                  <Translate contentKey="gatewayApp.paymentPayment.expiresAt">Expires At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('expiresAt')} />
                </th>
                <th className="hand" onClick={sort('createdAt')}>
                  <Translate contentKey="gatewayApp.paymentPayment.createdAt">Created At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('createdAt')} />
                </th>
                <th className="hand" onClick={sort('updatedAt')}>
                  <Translate contentKey="gatewayApp.paymentPayment.updatedAt">Updated At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('updatedAt')} />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {paymentList.map(payment => (
                <tr key={`entity-${payment.id}`} data-cy="entityTable">
                  <td>
                    <Button as={Link as any} to={`/payment/${payment.id}`} variant="link" size="sm">
                      {payment.id}
                    </Button>
                  </td>
                  <td>{payment.orderId}</td>
                  <td>{payment.orderCode}</td>
                  <td>{payment.amount}</td>
                  <td>{payment.currency}</td>
                  <td>
                    <Translate contentKey={`gatewayApp.PaymentProvider.${payment.provider}`} />
                  </td>
                  <td>
                    <Translate contentKey={`gatewayApp.PaymentStatus.${payment.status}`} />
                  </td>
                  <td>{payment.providerRef}</td>
                  <td>{payment.providerTransactionId}</td>
                  <td>{payment.paymentUrl}</td>
                  <td>{payment.failureReason}</td>
                  <td>{payment.expiresAt ? <TextFormat type="date" value={payment.expiresAt} format={APP_DATE_FORMAT} /> : null}</td>
                  <td>{payment.createdAt ? <TextFormat type="date" value={payment.createdAt} format={APP_DATE_FORMAT} /> : null}</td>
                  <td>{payment.updatedAt ? <TextFormat type="date" value={payment.updatedAt} format={APP_DATE_FORMAT} /> : null}</td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button as={Link as any} to={`/payment/${payment.id}`} variant="info" size="sm" data-cy="entityDetailsButton">
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
              <Translate contentKey="gatewayApp.paymentPayment.home.notFound">No Payments found</Translate>
            </div>
          )
        )}
      </div>
      {totalItems ? (
        <div className={paymentList && paymentList.length > 0 ? '' : 'd-none'}>
          <div className="justify-content-center d-flex">
            <JhiItemCount page={paginationState.activePage} total={totalItems} itemsPerPage={paginationState.itemsPerPage} i18nEnabled />
          </div>
          <div className="justify-content-center d-flex">
            <JhiPagination
              activePage={paginationState.activePage}
              onSelect={handlePagination}
              maxButtons={5}
              itemsPerPage={paginationState.itemsPerPage}
              totalItems={totalItems}
            />
          </div>
        </div>
      ) : (
        ''
      )}
    </div>
  );
};

export default Payment;
