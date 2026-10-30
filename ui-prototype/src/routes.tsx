import { createBrowserRouter } from 'react-router-dom';
import { PublicLayout } from './components/PublicLayout';
import { AdminLayout } from './components/AdminLayout';

// Features
import { HomePage } from './features/home/HomePage';
import { EventListingPage } from './features/events/EventListingPage';
import { EventDetailPage } from './features/events/EventDetailPage';
import { WaitingRoomPage } from './features/queue/WaitingRoomPage';
import { CheckoutPage } from './features/checkout/CheckoutPage';
import { OrderResultPage } from './features/checkout/OrderResultPage';
import { MyOrdersPage } from './features/orders/MyOrdersPage';
import { TicketWalletPage } from './features/wallet/TicketWalletPage';
import { AccountPage } from './features/account/AccountPage';
import { DesignSystemPage } from './features/designSystem/DesignSystemPage';

// Admin Features
import { AdminDashboardPage } from './features/admin/AdminDashboardPage';
import { AdminEventsPage } from './features/admin/AdminEventsPage';
import { AdminInventoryPage } from './features/admin/AdminInventoryPage';
import { AdminOrdersPage } from './features/admin/AdminOrdersPage';
import { AdminPaymentsPage } from './features/admin/AdminPaymentsPage';
import { AdminCheckInPage } from './features/admin/AdminCheckInPage';

// System Pages
import { NotFoundPage, ForbiddenPage, MaintenancePage } from './features/system/SystemPages';

export const router = createBrowserRouter([
  // Public Buyer Routes
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'events', element: <EventListingPage /> },
      { path: 'events/:slug', element: <EventDetailPage /> },
      { path: 'queue', element: <WaitingRoomPage /> },
      { path: 'checkout', element: <CheckoutPage /> },
      { path: 'order-result', element: <OrderResultPage /> },
      { path: 'my-orders', element: <MyOrdersPage /> },
      { path: 'wallet', element: <TicketWalletPage /> },
      { path: 'account', element: <AccountPage /> },
      { path: 'design-system', element: <DesignSystemPage /> },
      { path: 'forbidden', element: <ForbiddenPage /> },
      { path: 'maintenance', element: <MaintenancePage /> },
      { path: '*', element: <NotFoundPage /> }
    ]
  },

  // Admin & Organizer Routes
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: 'events', element: <AdminEventsPage /> },
      { path: 'inventory', element: <AdminInventoryPage /> },
      { path: 'orders', element: <AdminOrdersPage /> },
      { path: 'payments', element: <AdminPaymentsPage /> },
      { path: 'check-in', element: <AdminCheckInPage /> }
    ]
  }
]);
