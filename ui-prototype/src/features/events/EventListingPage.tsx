import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { Filter, X } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { Event, EventStatus } from '../../types/models';
import { EventPoster } from '../../components/EventPoster';
import { StatusBadge } from '../../components/StatusBadge';
import { formatDateTimeVN, formatVND } from '../../lib/format';
import { EmptyState, ErrorState } from '../../components/DataStates';

export const EventListingPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState(searchParams.get('q') || '');
  const [cityFilter, setCityFilter] = useState(searchParams.get('city') || '');
  const [categoryFilter, setCategoryFilter] = useState(searchParams.get('cat') || '');
  const [statusFilter, setStatusFilter] = useState<EventStatus | ''>('');
  const [priceMax, setPriceMax] = useState<number>(3000000);
  const [sortBy, setSortBy] = useState<'date-asc' | 'date-desc' | 'price-asc' | 'price-desc'>('date-asc');

  // Mobile bottom sheet filter toggle
  const [showMobileFilter, setShowMobileFilter] = useState<boolean>(false);

  useEffect(() => {
    loadEvents();
  }, []);

  const loadEvents = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await ApiService.getEvents();
      setEvents(data);
    } catch (e: unknown) {
      const err = e as { message?: string };
      setError(err.message || 'Không thể tải sự kiện.');
    } finally {
      setLoading(false);
    }
  };

  const filteredEvents = useMemo(() => {
    return events.filter(e => {
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const matchesName = e.name.toLowerCase().includes(q);
        const matchesDesc = e.description.toLowerCase().includes(q);
        const matchesVenue = e.venue.name.toLowerCase().includes(q);
        if (!matchesName && !matchesDesc && !matchesVenue) return false;
      }
      if (cityFilter && e.venue.city !== cityFilter) return false;
      if (categoryFilter && e.category !== categoryFilter) return false;
      if (statusFilter && e.status !== statusFilter) return false;

      // Price check
      const minPrice = e.ticketTypes.length > 0 ? Math.min(...e.ticketTypes.map(t => t.price)) : 0;
      if (minPrice > priceMax) return false;

      return true;
    }).sort((a, b) => {
      const minA = a.ticketTypes.length > 0 ? Math.min(...a.ticketTypes.map(t => t.price)) : 0;
      const minB = b.ticketTypes.length > 0 ? Math.min(...b.ticketTypes.map(t => t.price)) : 0;

      if (sortBy === 'date-asc') return new Date(a.startTime).getTime() - new Date(b.startTime).getTime();
      if (sortBy === 'date-desc') return new Date(b.startTime).getTime() - new Date(a.startTime).getTime();
      if (sortBy === 'price-asc') return minA - minB;
      if (sortBy === 'price-desc') return minB - minA;
      return 0;
    });
  }, [events, searchQuery, cityFilter, categoryFilter, statusFilter, priceMax, sortBy]);

  const resetFilters = () => {
    setSearchQuery('');
    setCityFilter('');
    setCategoryFilter('');
    setStatusFilter('');
    setPriceMax(3000000);
    setSearchParams({});
  };

  return (
    <div className="tr-container py-4">
      {/* Header Bar */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <h1 className="fs-3 fw-bold mb-1">Khám phá tất cả sự kiện</h1>
          <p className="text-muted small mb-0">Tìm kiếm vé hòa nhạc, lễ hội và sân khấu trên toàn quốc</p>
        </div>

        <div className="d-flex align-items-center gap-2">
          {/* Mobile Filter Button */}
          <button
            className="btn btn-tr-secondary d-md-none d-flex align-items-center gap-2"
            onClick={() => setShowMobileFilter(true)}
          >
            <Filter size={16} /> Bộ lọc
          </button>

          {/* Sort dropdown */}
          <div className="d-flex align-items-center gap-2">
            <span className="small text-muted d-none d-sm-inline">Sắp xếp:</span>
            <select
              className="form-select form-select-sm"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as typeof sortBy)}
              style={{ width: 'auto' }}
            >
              <option value="date-asc">Ngày diễn ra (gần nhất)</option>
              <option value="date-desc">Ngày diễn ra (xa nhất)</option>
              <option value="price-asc">Giá vé (thấp đến cao)</option>
              <option value="price-desc">Giá vé (cao đến thấp)</option>
            </select>
          </div>
        </div>
      </div>

      <div className="row g-4">
        {/* Desktop Sidebar Filters */}
        <div className="col-12 col-md-3 d-none d-md-block">
          <div className="tr-card p-3">
            <div className="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom border-subtle">
              <span className="fw-semibold">Bộ lọc tìm kiếm</span>
              <button onClick={resetFilters} className="btn btn-sm btn-link text-decoration-none p-0 small">
                Đặt lại
              </button>
            </div>

            {/* Keyword */}
            <div className="mb-3">
              <label className="form-label small fw-semibold text-muted" htmlFor="sidebarSearch">Từ khoá</label>
              <input
                id="sidebarSearch"
                type="text"
                className="form-control form-control-sm"
                placeholder="Tên sự kiện, nghệ sĩ..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            {/* City */}
            <div className="mb-3">
              <label className="form-label small fw-semibold text-muted" htmlFor="sidebarCity">Thành phố</label>
              <select
                id="sidebarCity"
                className="form-select form-select-sm"
                value={cityFilter}
                onChange={(e) => setCityFilter(e.target.value)}
              >
                <option value="">Tất cả thành phố</option>
                <option value="Hà Nội">Hà Nội</option>
                <option value="TP. Hồ Chí Minh">TP. Hồ Chí Minh</option>
                <option value="Đà Nẵng">Đà Nẵng</option>
              </select>
            </div>

            {/* Category */}
            <div className="mb-3">
              <label className="form-label small fw-semibold text-muted" htmlFor="sidebarCategory">Thể loại</label>
              <select
                id="sidebarCategory"
                className="form-select form-select-sm"
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
              >
                <option value="">Tất cả thể loại</option>
                <option value="Concert">Concert</option>
                <option value="Festival">Festival</option>
                <option value="Sân khấu">Sân khấu</option>
                <option value="Thể thao">Thể thao</option>
                <option value="Workshop">Workshop</option>
              </select>
            </div>

            {/* Status */}
            <div className="mb-3">
              <label className="form-label small fw-semibold text-muted" htmlFor="sidebarStatus">Trạng thái bán</label>
              <select
                id="sidebarStatus"
                className="form-select form-select-sm"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as EventStatus | '')}
              >
                <option value="">Tất cả</option>
                <option value="PUBLISHED">Đang mở bán</option>
                <option value="DRAFT">Sắp mở bán</option>
                <option value="ENDED">Đã kết thúc</option>
              </select>
            </div>

            {/* Price Max slider */}
            <div className="mb-2">
              <div className="d-flex justify-content-between small text-muted mb-1">
                <span>Mức giá tối đa</span>
                <span className="fw-bold text-main tabular-nums">{formatVND(priceMax)}</span>
              </div>
              <input
                type="range"
                className="form-range"
                min="300000"
                max="3000000"
                step="100000"
                value={priceMax}
                onChange={(e) => setPriceMax(Number(e.target.value))}
              />
            </div>
          </div>
        </div>

        {/* Results List */}
        <div className="col-12 col-md-9">
          {error && <ErrorState message={error} onRetry={loadEvents} />}

          {loading ? (
            <div className="row g-3">
              {[1, 2, 3, 4, 5, 6].map(n => (
                <div key={n} className="col-12 col-sm-6">
                  <div className="tr-card p-0">
                    <div className="tr-skeleton w-100" style={{ height: 160 }} />
                    <div className="p-3">
                      <div className="tr-skeleton w-75 mb-2" style={{ height: 18 }} />
                      <div className="tr-skeleton w-50" style={{ height: 14 }} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : filteredEvents.length === 0 ? (
            <EmptyState
              title="Không tìm thấy sự kiện phù hợp"
              description="Hãy thử thay đổi từ khoá tìm kiếm, thành phố hoặc khoảng giá lọc."
              actionText="Đặt lại bộ lọc"
              onAction={resetFilters}
            />
          ) : (
            <div className="row g-3">
              {filteredEvents.map(ev => {
                const minPrice = ev.ticketTypes.length > 0
                  ? Math.min(...ev.ticketTypes.map(t => t.price))
                  : 0;
                return (
                  <div key={ev.id} className="col-12 col-sm-6 col-lg-4">
                    <div className="tr-card tr-card-hover h-100 d-flex flex-column">
                      <EventPoster
                        title={ev.name}
                        category={ev.category}
                        dateStr={formatDateTimeVN(ev.startTime)}
                        venueName={ev.venue.name}
                        bgConfig={ev.posterBg}
                        height={160}
                      />
                      <div className="p-3 d-flex flex-column flex-grow-1 justify-content-between">
                        <div>
                          <div className="d-flex align-items-center justify-content-between mb-2">
                            <StatusBadge type="event" status={ev.status} />
                            <span className="small text-muted">{ev.venue.city}</span>
                          </div>
                          <h4 className="fs-6 fw-bold mb-2 text-truncate-2">
                            <Link to={`/events/${ev.slug}`} className="text-main text-decoration-none">
                              {ev.name}
                            </Link>
                          </h4>
                          <p className="small text-muted mb-3 text-truncate-2" style={{ fontSize: '0.8125rem' }}>
                            {ev.description}
                          </p>
                        </div>
                        <div className="pt-2 border-top border-subtle d-flex align-items-center justify-content-between mt-auto">
                          <div>
                            <span className="small text-muted" style={{ fontSize: '0.75rem' }}>Từ</span>
                            <div className="fw-bold text-primary tabular-nums fs-6">
                              {formatVND(minPrice)}
                            </div>
                          </div>
                          <Link to={`/events/${ev.slug}`} className="btn btn-sm btn-tr-secondary">
                            Chi tiết
                          </Link>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Mobile Bottom Sheet Filters */}
      {showMobileFilter && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-50 z-3 d-flex flex-column justify-content-end"
          onClick={() => setShowMobileFilter(false)}
        >
          <div
            className="bg-surface rounded-top-4 p-4 shadow-lg w-100"
            style={{ maxHeight: '85vh', overflowY: 'auto' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom border-subtle">
              <h5 className="fw-bold mb-0">Bộ lọc tìm kiếm</h5>
              <button className="btn btn-tr-ghost p-1" onClick={() => setShowMobileFilter(false)}>
                <X size={20} />
              </button>
            </div>

            <div className="mb-3">
              <label className="form-label small fw-semibold" htmlFor="mSearchInput">Từ khoá</label>
              <input
                id="mSearchInput"
                type="text"
                className="form-control"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className="mb-3">
              <label className="form-label small fw-semibold" htmlFor="mCitySelect">Thành phố</label>
              <select
                id="mCitySelect"
                className="form-select"
                value={cityFilter}
                onChange={(e) => setCityFilter(e.target.value)}
              >
                <option value="">Tất cả thành phố</option>
                <option value="Hà Nội">Hà Nội</option>
                <option value="TP. Hồ Chí Minh">TP. Hồ Chí Minh</option>
                <option value="Đà Nẵng">Đà Nẵng</option>
              </select>
            </div>

            <div className="mb-3">
              <label className="form-label small fw-semibold" htmlFor="mCategorySelect">Thể loại</label>
              <select
                id="mCategorySelect"
                className="form-select"
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
              >
                <option value="">Tất cả thể loại</option>
                <option value="Concert">Concert</option>
                <option value="Festival">Festival</option>
                <option value="Sân khấu">Sân khấu</option>
                <option value="Thể thao">Thể thao</option>
                <option value="Workshop">Workshop</option>
              </select>
            </div>

            <div className="d-flex gap-2 mt-4">
              <button className="btn btn-tr-secondary flex-grow-1" onClick={resetFilters}>
                Đặt lại
              </button>
              <button className="btn btn-tr-primary flex-grow-1" onClick={() => setShowMobileFilter(false)}>
                Áp dụng kết quả ({filteredEvents.length})
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
