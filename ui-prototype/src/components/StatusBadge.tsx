import React from 'react';
import type { EventStatus, OrderStatus, PaymentStatus } from '../types/models';

interface StatusBadgeProps {
  status: EventStatus | OrderStatus | PaymentStatus | string;
  type?: 'event' | 'order' | 'payment';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const getConfig = (): { label: string; className: string } => {
    switch (status) {
      // Event statuses
      case 'PUBLISHED':
        return { label: 'Đang mở bán', className: 'badge-success' };
      case 'DRAFT':
        return { label: 'Bản nháp', className: 'badge-neutral' };
      case 'ENDED':
        return { label: 'Đã kết thúc', className: 'badge-neutral' };
      case 'CANCELLED':
        return { label: 'Đã huỷ', className: 'badge-danger' };

      // Order statuses
      case 'PENDING':
        return { label: 'Đang xử lý', className: 'badge-info' };
      case 'RESERVED':
        return { label: 'Đang giữ ghế (10p)', className: 'badge-warning' };
      case 'AWAITING_PAYMENT':
        return { label: 'Chờ thanh toán', className: 'badge-warning' };
      case 'CONFIRMED':
        return { label: 'Đã xác nhận', className: 'badge-success' };
      case 'EXPIRED':
        return { label: 'Hết hạn giữ chỗ', className: 'badge-neutral' };
      case 'FAILED':
        return { label: 'Thất bại', className: 'badge-danger' };
      case 'REFUNDED':
        return { label: 'Đã hoàn tiền', className: 'badge-info' };

      // Payment statuses
      case 'INITIATED':
        return { label: 'Khởi tạo', className: 'badge-info' };
      case 'PROCESSING':
        return { label: 'Đang thanh toán', className: 'badge-warning' };
      case 'SUCCEEDED':
        return { label: 'Thành công', className: 'badge-success' };

      default:
        return { label: status, className: 'badge-neutral' };
    }
  };

  const { label, className } = getConfig();
  return <span className={`tr-badge ${className}`}>{label}</span>;
};
