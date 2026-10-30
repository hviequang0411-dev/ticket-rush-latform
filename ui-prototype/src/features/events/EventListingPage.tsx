import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Search, SlidersHorizontal, MapPin, Calendar, X, RotateCcw, ArrowUpDown } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { Event, EventStatus } from '../../types/models';
import { EventPoster } from '../../components/EventPoster';
import { StatusBadge } from '../../components/StatusBadge';
import { EventCardSkeleton, EmptyState, ErrorState } from '../../components/DataStates';
import { formatVND, formatDateTimeVN, formatDateOnlyVN } from '../../lib/format';

type CategoryFilter = Event['category'] | 'ALL';

function minTicketPrice(event: Event): number {
  if (event.ticketTypes.length === 0) return 0;
  return Math.min(...event.ticketTypes.map(t => t.price));
}

export const EventListingPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const [allEvents, setAllEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filter states initialized from URL params
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [city, setCity] = useState(searchParams.get('city') || 'ALL');
  const [category, setCategory] = useState<CategoryFilter>(
    (searchParams.get('category') as CategoryFilter) || 'ALL'
  );
  const [status, setStatus] = useState<EventStatus | 'ALL'>('ALL');
  const [maxPrice, setMaxPrice] = useState<number>(5000000);
  const [sortBy, setSortBy] = useState<'date' | 'price-asc' | 'price-desc'>('date');

  // Mobile drawer state
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  const loadEvents = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await ApiService.getEvents();
      setAllEvents(data);
    } catch (err: unknown) {
      const e = err as { detail?: string; message?: string };
      setError(e.detail || e.message || 'Lỗi khi lọc danh sách sự kiện.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEvents();
  }, []);

  const events = allEvents
    .filter(ev => {
      if (keyword.trim()) {
        const q = keyword.trim().toLowerCase();
        if (!ev.name.toLowerCase().includes(q) && !ev.venue.name.toLowerCase().includes(q)) return false;
      }
      if (city !== 'ALL' && ev.venue.city !== city) return false;
      if (category !== 'ALL' && ev.category !== category) return false;
      if (status !== 'ALL' && ev.status !== status) return false;
      if (minTicketPrice(ev) > maxPrice) return false;
      return true;
    })
    .sort((a, b) => {
      if (sortBy === 'price-asc') return minTicketPrice(a) - minTicketPrice(b);
      if (sortBy === 'price-desc') return minTicketPrice(b) - minTicketPrice(a);
      return new Date(a.startTime).getTime() - new Date(b.startTime).getTime();
    });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
  };

  const handleResetFilters = () => {
    setKeyword('');
    setCity('ALL');
    setCategory('ALL');
    setStatus('ALL');
    setMaxPrice(5000000);
    setSortBy('date');
    setSearchParams({});
  };

  const renderFilterControls = () => (
    <div className="d-flex flex-column gap-4">
      <div>
        <label className="form-label fw-semibold small text-muted" htmlFor="filterCity">
          Thành phố tổ chức
        </label>
        <select
          id="filterCity"
          className="form-select"
          value={city}
          onChange={(e) => setCity(e.target.value)}
        >
          <option value="ALL">Tất cả thành phố</option>
          <option value="Hà Nội">Hà Nội</option>
          <option value="TP. Hồ Chí Minh">TP. Hồ Chí Minh</option>
          <option value="Đà Nẵng">Đà Nẵng</option>
        </select>
      </div>

      <div>
        <label className="form-label fw-semibold small text-muted" htmlFor="filterCategory">
          Danh mục sự kiện
        </label>
        <select
          id="filterCategory"
          className="form-select"
          value={category}
          onChange={(e) => setCategory(e.target.value as CategoryFilter)}
        >
          <option value="ALL">Tất cả thể loại</option>
          <option value="Concert">Concert / Đêm nhạc</option>
          <option value="Festival">Festival / Lễ hội</option>
          <option value="Sân khấu">Sân khấu &amp; Nghệ thuật</option>
          <option value="Thể thao">Thể thao</option>
          <option value="Workshop">Hội thảo &amp; Workshop</option>
        </select>
      </div>

      <div>
        <label className="form-label fw-semibold small text-muted" htmlFor="filterStatus">
          Trạng thái mở bán
        </label>
        <select
          id="filterStatus"
          className="form-select"
          value={status}
          onChange={(e) => setStatus(e.target.value as EventStatus | 'ALL')}
        >
          <option value="ALL">Tất cả trạng thái</option>
          <option value="PUBLISHED">Đang mở bán</option>
          <option value="DRAFT">Sắp mở bán (Nháp)</option>
          <option value="ENDED">Đã kết thúc</option>
        </select>
      </div>

      <div>
        <div className="d-flex justify-content-between align-items-center mb-1">
          <label className="form-label fw-semibold small text-muted mb-0" htmlFor="filterPrice">
            Mức giá tối đa
          </label>
          <span className="small fw-bold text-primary tabular-nums">{formatVND(maxPrice)}</span>
        </div>
        <input
          id="filterPrice"
          type="range"
          className="form-range"
          min={200000}
          max={5000000}
          step={100000}
          value={maxPrice}
          onChange={(e) => setMaxPrice(Number(e.target.value))}
        />
        <div className="d-flex justify-content-between text-muted" style={{ fontSize: '0.72rem' }}>
          <span>200.000 ₫</span>
          <span>5.000.000 ₫</span>
        </div>
      </div>

      <button
        type="button"
        onClick={handleResetFilters}
        className="btn btn-tr-secondary w-100 d-flex align-items-center justify-content-center gap-2"
      >
        <RotateCcw size={16} />
        <span>Đặt lại bộ lọc</span>
      </button>
    </div>
  );

  return (
    <div className="tr-container py-4">
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3 pb-3 border-bottom border-subtle">
        <div>
          <h1 className="fs-3 fw-bold mb-1">Khám phá &amp; Tìm kiếm Sự kiện</h1>
          <p className="text-muted small mb-0">
            Hiển thị <strong className="text-main tabular-nums">{events.length}</strong> sự kiện phù hợp với tiêu chí của bạn
          </p>
        </div>

        <div className="d-flex flex-wrap align-items-center gap-2">
          <form onSubmit={handleSearchSubmit} className="d-flex gap-2 flex-grow-1">
            <div className="input-group">
              <span className="input-group-text border-end-0">
                <Search size={16} className="text-muted" />
              </span>
              <input
                type="text"
                className="form-control border-start-0"
                placeholder="Tìm sự kiện, địa điểm..."
                aria-label="Từ khoá tìm kiếm"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn-tr-primary flex-shrink-0">
              Tìm
            </button>
          </form>

          <button
            type="button"
            className="btn btn-tr-secondary d-lg-none"
            onClick={() => setShowMobileFilters(true)}
          >
            <SlidersHorizontal size={16} />
            <span>Bộ lọc</span>
          </button>
        </div>
      </div>

      <div className="row g-4">
        <aside className="col-lg-3 d-none d-lg-block">
          <div className="tr-card p-4 sticky-top" style={{ top: '84px' }}>
            <div className="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom border-subtle">
              <SlidersHorizontal size={18} className="text-primary" />
              <h2 className="fs-6 fw-bold mb-0">Bộ lọc tìm kiếm</h2>
            </div>
            {renderFilterControls()}
          </div>
        </aside>

        <div className="col-12 col-lg-9">
          <div className="tr-card p-3 mb-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
            <div className="d-flex flex-wrap align-items-center gap-2">
              {city !== 'ALL' && (
                <span className="tr-badge badge-info">
                  Khu vực: {city}
                </span>
              )}
              {category !== 'ALL' && (
                <span className="tr-badge badge-warning">
                  Thể loại: {category}
                </span>
              )}
              {city === 'ALL' && category === 'ALL' && (
                <span className="small text-muted">Đang hiển thị tất cả khu vực &amp; thể loại</span>
              )}
            </div>
            <div className="d-flex align-items-center gap-2 ms-auto">
              <ArrowUpDown size={15} className="text-muted" />
              <label htmlFor="sortSelect" className="small text-muted mb-0">Sắp xếp:</label>
              <select
                id="sortSelect"
                className="form-select form-select-sm w-auto"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as 'date' | 'price-asc' | 'price-desc')}
              >
                <option value="date">Ngày diễn ra gần nhất</option>
                <option value="price-asc">Giá vé: Thấp đến Cao</option>
                <option value="price-desc">Giá vé: Cao đến Thấp</option>
              </select>
            </div>
          </div>

          {error && <ErrorState message={error} onRetry={loadEvents} />}

          {loading ? (
            <div className="row g-4">
              {[1, 2, 3, 4, 5, 6].map((n) => (
                <div key={n} className="col-12 col-md-6">
                  <EventCardSkeleton />
                </div>
              ))}
            </div>
          ) : events.length === 0 ? (
            <EmptyState
              title="Không tìm thấy sự kiện phù hợp"
              description="Không có sự kiện nào khớp với bộ lọc hiện tại của bạn. Hãy thử nới lỏng khoảng giá hoặc chọn tất cả thành phố."
              actionText="Xoá bộ lọc"
              onAction={handleResetFilters}
            />
          ) : (
            <div className="row g-4">
              {events.map((event) => (
                <div key={event.id} className="col-12 col-md-6">
                  <Link
                    to={`/events/${event.slug}`}
                    className="tr-card tr-card-interactive overflow-hidden h-100 d-flex flex-column text-decoration-none"
                  >
                    <EventPoster
                      title={event.name}
                      category={event.category}
                      dateText={formatDateOnlyVN(event.startTime)}
                      city={event.venue.city}
                      posterBg={event.posterBg}
                      height={195}
                    />
                    <div className="p-3 p-md-4 d-flex flex-column flex-grow-1 justify-content-between">
                      <div>
                        <div className="d-flex justify-content-between align-items-center mb-2">
                          <span className="small text-muted d-flex align-items-center gap-1 tabular-nums">
                            <Calendar size={14} className="text-primary" />
                            {formatDateTimeVN(event.startTime)}
                          </span>
                          <StatusBadge type="event" status={event.status} />
                        </div>
                        <h3 className="fs-6 fw-bold text-main mb-2">{event.name}</h3>
                        <p className="small text-muted mb-3 d-flex align-items-center gap-1">
                          <MapPin size={14} className="flex-shrink-0" />
                          <span className="text-truncate">{event.venue.name}, {event.venue.city}</span>
                        </p>
                      </div>

                      <div className="pt-3 border-top border-subtle d-flex justify-content-between align-items-center">
                        <div>
                          <span className="text-muted d-block" style={{ fontSize: '0.72rem' }}>Giá từ</span>
                          <span className="fw-bold text-primary tabular-nums">{formatVND(minTicketPrice(event))}</span>
                        </div>
                        <span className="btn btn-sm btn-tr-secondary py-1 px-3">Chọn vé</span>
                      </div>
                    </div>
                  </Link>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {showMobileFilters && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-50 d-lg-none"
          style={{ zIndex: 1050, backdropFilter: 'blur(4px)' }}
          onClick={() => setShowMobileFilters(false)}
        >
          <div
            className="position-absolute bottom-0 start-0 w-100 bg-surface rounded-top-4 p-4 shadow-lg"
            style={{ maxHeight: '85vh', overflowY: 'auto' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom border-subtle">
              <h3 className="fs-5 fw-bold mb-0">Bộ lọc sự kiện</h3>
              <button
                type="button"
                className="btn-tr-ghost p-1"
                onClick={() => setShowMobileFilters(false)}
                aria-label="Đóng bộ lọc"
              >
                <X size={20} />
              </button>
            </div>
            {renderFilterControls()}
            <button
              type="button"
              className="btn btn-tr-primary w-100 mt-3"
              onClick={() => setShowMobileFilters(false)}
            >
              Áp dụng ({events.length} kết quả)
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
