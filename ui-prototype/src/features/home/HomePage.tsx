import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Search, MapPin, Calendar, Flame, ChevronRight, ArrowRight } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { Event } from '../../types/models';
import { EventPoster } from '../../components/EventPoster';
import { formatDateTimeVN, formatVND } from '../../lib/format';
import { StatusBadge } from '../../components/StatusBadge';
import { ErrorState } from '../../components/DataStates';

export const HomePage: React.FC = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Search Hero State
  const [keyword, setKeyword] = useState('');
  const [selectedCity, setSelectedCity] = useState('');
  const [activeHeroIndex, setActiveHeroIndex] = useState(0);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await ApiService.getEvents();
      setEvents(data);
    } catch (e: unknown) {
      const err = e as { message?: string };
      setError(err.message || 'Không thể tải danh sách sự kiện.');
    } finally {
      setLoading(false);
    }
  };

  const hotEvents = events.filter(e => e.isHot || e.status === 'PUBLISHED').slice(0, 4);
  const heroEvents = events.slice(0, 3);

  // Auto carousel for hero
  useEffect(() => {
    if (heroEvents.length === 0) return;
    const interval = setInterval(() => {
      setActiveHeroIndex(prev => (prev + 1) % heroEvents.length);
    }, 5000);
    return () => clearInterval(interval);
  }, [heroEvents.length]);

  const categories = ['Tất cả', 'Concert', 'Festival', 'Sân khấu', 'Thể thao', 'Workshop'];
  const cities = ['Tất cả thành phố', 'Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng'];

  return (
    <div>
      {/* 1. Hero Showcase Carousel */}
      <section className="position-relative bg-dark text-white overflow-hidden" aria-label="Sự kiện tiêu điểm">
        {heroEvents.length > 0 && (
          <div className="position-relative" style={{ minHeight: '520px' }}>
            {heroEvents.map((ev, idx) => (
              <div
                key={ev.id}
                className={`position-absolute top-0 start-0 w-100 h-100 d-flex align-items-center transition-opacity ${
                  idx === activeHeroIndex ? 'opacity-100 z-1' : 'opacity-0 z-0'
                }`}
                style={{
                  background: ev.posterBg.gradient,
                  transition: 'opacity 500ms ease-in-out'
                }}
              >
                <div className="tr-container py-5" style={{ paddingBottom: '5rem' }}>
                  <div className="row align-items-center g-4">
                    <div className="col-12 col-lg-7">
                      <div className="d-flex align-items-center gap-2 mb-3">
                        <span className="badge bg-warning text-dark fw-bold px-2 py-1 d-inline-flex align-items-center gap-1">
                          <Flame size={14} /> TIÊU ĐIỂM
                        </span>
                        <span className="badge bg-white bg-opacity-25 text-white">{ev.category}</span>
                      </div>
                      <h1 className="display-5 fw-bold mb-3 text-white" style={{ letterSpacing: '-0.5px' }}>
                        {ev.name}
                      </h1>
                      <p className="lead text-white-50 mb-4" style={{ maxWidth: '580px' }}>
                        {ev.description}
                      </p>
                      <div className="d-flex flex-wrap align-items-center gap-4 mb-4 small text-white-50">
                        <div className="d-flex align-items-center gap-2">
                          <Calendar size={18} className="text-warning" />
                          <span>{formatDateTimeVN(ev.startTime)}</span>
                        </div>
                        <div className="d-flex align-items-center gap-2">
                          <MapPin size={18} className="text-warning" />
                          <span>{ev.venue.name}, {ev.venue.city}</span>
                        </div>
                      </div>
                      <div className="d-flex align-items-center gap-3">
                        <Link to={`/events/${ev.slug}`} className="btn btn-tr-primary px-4 py-2">
                          Đặt vé ngay <ArrowRight size={18} />
                        </Link>
                        <Link to={`/queue?event=${ev.slug}`} className="btn btn-outline-light d-none d-sm-inline-flex btn-touch-target">
                          Thử phòng chờ ảo
                        </Link>
                      </div>
                    </div>

                    <div className="col-12 col-lg-5 d-none d-lg-block">
                      <div className="rounded-3 overflow-hidden shadow-lg border border-white border-opacity-10">
                        <EventPoster
                          title={ev.name}
                          category={ev.category}
                          dateStr={formatDateTimeVN(ev.startTime)}
                          venueName={ev.venue.name}
                          bgConfig={ev.posterBg}
                          height={300}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}

            {/* Carousel navigation dots */}
            <div className="position-absolute bottom-0 start-50 translate-middle-x mb-2 z-2 d-flex gap-2">
              {heroEvents.map((_, i) => (
                <button
                  key={i}
                  onClick={() => setActiveHeroIndex(i)}
                  className={`border-0 rounded-pill p-0 transition-all ${i === activeHeroIndex ? 'bg-primary' : 'bg-white bg-opacity-50'}`}
                  style={{ width: i === activeHeroIndex ? 24 : 8, height: 8 }}
                  aria-label={`Chuyển tới slide ${i + 1}`}
                />
              ))}
            </div>
          </div>
        )}
      </section>

      {/* 2. Floating Search Bar */}
      <section className="position-relative py-4" style={{ zIndex: 10 }}>
        <div className="tr-container">
          <div className="tr-card p-3 p-md-4 shadow-md bg-surface">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                window.location.href = `/events?q=${encodeURIComponent(keyword)}&city=${encodeURIComponent(selectedCity)}`;
              }}
              className="row g-2 g-md-3 align-items-center"
            >
              <div className="col-12 col-md-5">
                <label className="form-label small fw-semibold text-muted mb-1" htmlFor="keywordInput">Từ khoá sự kiện / Nghệ sĩ</label>
                <div className="input-group">
                  <span className="input-group-text bg-surface-subtle border-end-0">
                    <Search size={18} className="text-muted" />
                  </span>
                  <input
                    id="keywordInput"
                    type="text"
                    className="form-control border-start-0"
                    placeholder="Tìm tên đêm nhạc, lễ hội..."
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                  />
                </div>
              </div>

              <div className="col-12 col-sm-6 col-md-4">
                <label className="form-label small fw-semibold text-muted mb-1" htmlFor="citySelect">Khu vực / Thành phố</label>
                <div className="input-group">
                  <span className="input-group-text bg-surface-subtle border-end-0">
                    <MapPin size={18} className="text-muted" />
                  </span>
                  <select
                    id="citySelect"
                    className="form-select border-start-0"
                    value={selectedCity}
                    onChange={(e) => setSelectedCity(e.target.value)}
                  >
                    {cities.map((c) => (
                      <option key={c} value={c === 'Tất cả thành phố' ? '' : c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="col-12 col-sm-6 col-md-3 d-flex align-items-end">
                <button type="submit" className="btn btn-tr-primary w-100">
                  <Search size={18} /> Tìm vé ngay
                </button>
              </div>
            </form>
          </div>
        </div>
      </section>

      {/* 3. Category Filter Chips */}
      <section className="py-4">
        <div className="tr-container">
          <div className="d-flex align-items-center gap-2 overflow-auto pb-2" style={{ whiteSpace: 'nowrap' }}>
            {categories.map((cat, i) => (
              <Link
                key={cat}
                to={cat === 'Tất cả' ? '/events' : `/events?cat=${cat}`}
                className={`btn btn-sm btn-touch-target rounded-pill px-3 py-1 text-decoration-none ${
                  i === 0 ? 'btn-tr-primary' : 'btn-tr-secondary'
                }`}
              >
                {cat}
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* 4. Đang Hot & Đang mở bán */}
      <section className="py-4">
        <div className="tr-container">
          <div className="d-flex align-items-center justify-content-between mb-4">
            <div>
              <div className="d-flex align-items-center gap-2">
                <Flame size={24} className="text-danger" />
                <h2 className="fs-3 fw-bold mb-0">Sự kiện nổi bật</h2>
              </div>
              <p className="text-muted small mb-0">Các concert và sự kiện có lượng đặt chỗ cao trong 24 giờ qua</p>
            </div>
            <Link to="/events" className="btn btn-tr-ghost d-flex align-items-center gap-1">
              Xem tất cả <ChevronRight size={16} />
            </Link>
          </div>

          {error && <ErrorState message={error} onRetry={loadData} />}

          {loading ? (
            <div className="row g-4">
              {[1, 2, 3, 4].map(n => (
                <div key={n} className="col-12 col-sm-6 col-lg-3">
                  <div className="tr-card p-0">
                    <div className="tr-skeleton w-100" style={{ height: 180 }} />
                    <div className="p-3">
                      <div className="tr-skeleton w-75 mb-2" style={{ height: 20 }} />
                      <div className="tr-skeleton w-50 mb-3" style={{ height: 16 }} />
                      <div className="tr-skeleton w-100" style={{ height: 36 }} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="row g-4">
              {hotEvents.map(ev => {
                const minPrice = ev.ticketTypes.length > 0
                  ? Math.min(...ev.ticketTypes.map(t => t.price))
                  : 0;
                return (
                  <div key={ev.id} className="col-12 col-sm-6 col-lg-3">
                    <div className="tr-card tr-card-hover h-100 d-flex flex-column">
                      <EventPoster
                        title={ev.name}
                        category={ev.category}
                        dateStr={formatDateTimeVN(ev.startTime)}
                        venueName={ev.venue.name}
                        bgConfig={ev.posterBg}
                        height={180}
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
                        </div>
                        <div className="pt-3 border-top border-subtle d-flex align-items-center justify-content-between mt-auto">
                          <div>
                            <div className="small text-muted" style={{ fontSize: '0.75rem' }}>Giá từ</div>
                            <div className="fw-bold text-primary tabular-nums fs-6">
                              {formatVND(minPrice)}
                            </div>
                          </div>
                          <Link to={`/events/${ev.slug}`} className="btn btn-sm btn-tr-secondary">
                            Xem vé
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
      </section>

      {/* 5. Khám phá theo thành phố */}
      <section className="py-5 bg-surface-subtle border-top border-bottom border-subtle">
        <div className="tr-container">
          <div className="text-center mb-4">
            <h2 className="fs-3 fw-bold mb-1">Khám phá theo địa điểm</h2>
            <p className="text-muted small">Tìm nhanh sự kiện tại các trung tâm văn hóa và giải trí lớn</p>
          </div>
          <div className="row g-3">
            {[
              { name: 'Hà Nội', count: '18+ Sự kiện', desc: 'Nhà Hát Lớn, Mỹ Đình, Phố Cổ' },
              { name: 'TP. Hồ Chí Minh', count: '24+ Sự kiện', desc: 'SECC, Sân khấu kịch, Phố Đi Bộ' },
              { name: 'Đà Nẵng', count: '9+ Sự kiện', desc: 'Bờ Đông Sông Hàn, Biển Đông, Bán Đảo' }
            ].map((city) => (
              <div key={city.name} className="col-12 col-md-4">
                <Link
                  to={`/events?city=${encodeURIComponent(city.name)}`}
                  className="tr-card p-4 d-block text-decoration-none tr-card-hover"
                >
                  <div className="d-flex align-items-center justify-content-between mb-2">
                    <h3 className="fs-5 fw-bold text-main mb-0">{city.name}</h3>
                    <span className="badge bg-primary-subtle text-primary">{city.count}</span>
                  </div>
                  <p className="text-muted small mb-0">{city.desc}</p>
                </Link>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
};

