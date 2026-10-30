import React from 'react';
import type { EventStatus, OrderStatus, PaymentStatus } from '../types/models';

export const StatusBadge: React.FC<{
  type: 'event' | 'order' | 'payment';
  status: EventStatus | OrderStatus | PaymentStatus;
}> = ({ type, status }) => {
  if (type === 'event') {
    switch (status as EventStatus) {
      case 'PUBLISHED':
        return <span className="tr-badge badge-success">Đang mở bán</span>;
      case 'DRAFT':
        return <span className="tr-badge badge-neutral">Sắp mở bán</span>;
      case 'ENDED':
        return <span className="tr-badge badge-neutral">Đã kết thúc</span>;
      case 'CANCELLED':
        return <span className="tr-badge badge-danger">Đã huỷ</span>;
      default:
        return <span className="tr-badge badge-neutral">{status}</span>;
    }
  }

  if (type === 'order') {
    switch (status as OrderStatus) {
      case 'CONFIRMED':
        return <span className="tr-badge badge-success">Đã xác nhận</span>;
      case 'AWAITING_PAYMENT':
        return <span className="tr-badge badge-warning">Chờ thanh toán</span>;
      case 'RESERVED':
        return <span className="tr-badge badge-info">Đang giữ chỗ</span>;
      case 'PENDING':
        return <span className="tr-badge badge-neutral">Đang xử lý</span>;
      case 'EXPIRED':
        return <span className="tr-badge badge-neutral">Đã hết hạn</span>;
      case 'CANCELLED':
        return <span className="tr-badge badge-danger">Đã huỷ</span>;
      case 'FAILED':
        return <span className="tr-badge badge-danger">Thất bại</span>;
      case 'REFUNDED':
        return <span className="tr-badge badge-neutral">Đã hoàn tiền</span>;
      default:
        return <span className="tr-badge badge-neutral">{status}</span>;
    }
  }

  if (type === 'payment') {
    switch (status as PaymentStatus) {
      case 'SUCCEEDED':
        return <span className="tr-badge badge-success">Thành công</span>;
      case 'PENDING':
        return <span className="tr-badge badge-warning">Đang chờ xử lý</span>;
      case 'FAILED':
        return <span className="tr-badge badge-danger">Thất bại</span>;
      case 'EXPIRED':
        return <span className="tr-badge badge-neutral">Hết hạn thanh toán</span>;
      case 'REFUNDED':
        return <span className="tr-badge badge-neutral">Đã hoàn tiền</span>;
      default:
        return <span className="tr-badge badge-neutral">{status}</span>;
    }
  }

  return <span className="tr-badge badge-neutral">{status}</span>;
};

