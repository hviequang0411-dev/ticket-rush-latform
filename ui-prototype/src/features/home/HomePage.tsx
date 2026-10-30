import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, MapPin, Calendar, Flame, ArrowRight, Sparkles, Music, Users, Mic2, Compass, Clock, ShieldCheck, Ticket } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { Event } from '../../types/models';
import { EventPoster } from '../../components/EventPoster';
import { EventCardSkeleton, ErrorState, EmptyState } from '../../components/DataStates';
import { formatVND, formatDateOnlyVN, formatDateTimeVN } from '../../lib/format';

type CategoryFilter = Event['category'] | 'ALL';

const CATEGORIES: { value: CategoryFilter; label: string; icon: React.ReactNode }[] = [
  { value: 'ALL', label: 'Tất cả', icon: <Compass size={16} /> },
  { value: 'Concert', label: 'Concert', icon: <Music size={16} /> },
  { value: 'Festival', label: 'Festival', icon: <Flame size={16} /> },
  { value: 'Sân khấu', label: 'Sân khấu', icon: <Mic2 size={16} /> },
  { value: 'Thể thao', label: 'Thể thao', icon: <Users size={16} /> },
  { value: 'Workshop', label: 'Workshop', icon: <Sparkles size={16} /> },
];

const CITIES = ['Tất cả thành phố', 'Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng'];

function minTicketPrice(event: Event): number {
  if (event.ticketTypes.length === 0) return 0;
  return Math.min(...event.ticketTypes.map(t => t.price));
}

const PROMISES = [
  { icon: <Clock size={20} />, title: 'Giữ ghế 10 phút', text: 'Ghế bạn chọn được giữ riêng cho bạn trong lúc thanh toán.' },
  { icon: <Ticket size={20} />, title: 'Vé điện tử QR', text: 'Vé có ngay trong ví sau khi thanh toán, quét tại cổng là vào.' },
  { icon: <ShieldCheck size={20} />, title: 'Tự hoàn tiền', text: 'Đơn không thành công vì hết ghế sẽ được hoàn tiền tự động.' },
];

export const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters on home
  const [activeCategory, setActiveCategory] = useState<CategoryFilter>('ALL');
  const [searchKeyword, setSearchKeyword] = useState<string>('');
  const [selectedCity, setSelectedCity] = useState<string>('Tất cả thành phố');

  // Hero carousel index
  const [heroIndex, setHeroIndex] = useState<number>(0);

  const fetchHomeData = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await ApiService.getEvents();
      setEvents(data);
    } catch (err: unknown) {
      const e = err as { detail?: string; message?: string };
      setError(e.detail || e.message || 'Không thể tải danh sách sự kiện.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHomeData();
  }, []);

  const featuredEvents = events.slice(0, 3);

  useEffect(() => {
    if (featuredEvents.length <= 1) return;
    const interval = setInterval(() => {
      setHeroIndex((prev) => (prev + 1) % featuredEvents.length);
    }, 6000);
    return () => clearInterval(interval);
  }, [featuredEvents.length]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (searchKeyword.trim()) params.set('keyword', searchKeyword.trim());
    if (selectedCity !== 'Tất cả thành phố') params.set('city', selectedCity);
    if (activeCategory !== 'ALL') params.set('category', activeCategory);
    navigate(`/events?${params.toString()}`);
  };

  const filteredEvents = events.filter(ev => {
    if (activeCategory !== 'ALL' && ev.category !== activeCategory) return false;
    return true;
  });

  const heroEvent = featuredEvents[heroIndex] || events[0];

  return (
    <div>
      {/* 1. CONCERT STAGE HERO BANNER WITH AMBIENT COLOR GLOW */}
      <section
        className="tr-hero-stage"
        style={{
          background: heroEvent ? heroEvent.posterBg.gradient : '#090d16',
          transition: 'background 0.6s ease',
        }}
      >
        <div
          className="position-absolute top-0 start-0 w-100 h-100 pointer-events-none"
          style={{
            background: 'linear-gradient(95deg, rgba(9, 13, 22, 0.88) 0%, rgba(9, 13, 22, 0.62) 55%, rgba(9, 13, 22, 0.35) 100%)',
          }}
        />
        {heroEvent && (
          <div
            className="tr-hero-glow"
            style={{
              top: '-180px',
              right: '8%',
              background: heroEvent.posterBg.accent,
            }}
          />
        )}

        <div className="tr-container py-5 position-relative z-1">
          {loading ? (
            <div className="row align-items-center g-4 py-3">
              <div className="col-12 col-lg-7">
                <div className="tr-skeleton w-25 mb-3" style={{ height: 28, opacity: 0.25 }} />
                <div className="tr-skeleton w-100 mb-2" style={{ height: 48, opacity: 0.25 }} />
                <div className="tr-skeleton w-75 mb-4" style={{ height: 24, opacity: 0.25 }} />
                <div className="tr-skeleton w-50" style={{ height: 48, opacity: 0.25 }} />
              </div>
              <div className="col-12 col-lg-5 d-none d-lg-block">
                <div className="tr-skeleton w-100 rounded-4" style={{ height: 320, opacity: 0.25 }} />
              </div>
            </div>
          ) : heroEvent ? (
            <div className="row align-items-center g-4 py-2">
              <div className="col-12 col-lg-7">
                <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
                  <span className="tr-hero-eyebrow">Sắp diễn ra · {heroEvent.category}</span>
                  {heroEvent.waitingRoom && <span className="tr-hero-eyebrow tr-hero-eyebrow-accent">Mở bán qua phòng chờ</span>}
                </div>

                <h1 className="tr-hero-title">{heroEvent.name}</h1>

                <p className="tr-hero-lead">{heroEvent.description}</p>

                <div className="d-flex flex-wrap align-items-center gap-4 mb-4 small" style={{ color: 'rgba(255,255,255,0.88)' }}>
                  <div className="d-flex align-items-center gap-2">
                    <Calendar size={18} className="text-warning" />
                    <span className="fw-medium tabular-nums">{formatDateTimeVN(heroEvent.startTime)}</span>
                  </div>
                  <div className="d-flex align-items-center gap-2">
                    <MapPin size={18} className="text-warning" />
                    <span className="fw-medium">{heroEvent.venue.name}, {heroEvent.venue.city}</span>
                  </div>
                </div>

                <div className="d-flex flex-wrap align-items-center gap-3">
                  <Link to={`/events/${heroEvent.slug}`} className="btn btn-tr-primary px-4 py-2 text-decoration-none">
                    <span>Xem vé</span>
                    <ArrowRight size={18} />
                  </Link>
                  {heroEvent.waitingRoom && (
                    <Link
                      to={`/queue?event=${heroEvent.slug}`}
                      className="btn btn-outline-light d-none d-sm-inline-flex btn-touch-target px-3 rounded-3 text-decoration-none"
                    >
                      Vào phòng chờ
                    </Link>
                  )}
                </div>

                {featuredEvents.length > 1 && (
                  <div className="d-flex align-items-center gap-2 mt-4 pt-1">
                    {featuredEvents.map((ev, idx) => (
                      <button
                        key={ev.id}
                        type="button"
                        onClick={() => setHeroIndex(idx)}
                        aria-label={`Xem sự kiện tiêu điểm: ${ev.name}`}
                        className="border-0 rounded-pill transition-all p-0"
                        style={{
                          width: idx === heroIndex ? 32 : 10,
                          height: 6,
                          backgroundColor: idx === heroIndex ? 'var(--color-primary)' : 'rgba(255,255,255,0.3)',
                          transition: 'all 0.25s ease',
                        }}
                      />
                    ))}
                  </div>
                )}
              </div>

              <div className="col-12 col-lg-5 d-none d-lg-block">
                <div
                  className="rounded-4 overflow-hidden"
                  style={{
                    border: '1px solid rgba(255,255,255,0.18)',
                    boxShadow: '0 20px 44px -12px rgba(0,0,0,0.65)',
                  }}
                >
                  <EventPoster
                    title={heroEvent.name}
                    category={heroEvent.category}
                    dateText={formatDateTimeVN(heroEvent.startTime)}
                    city={heroEvent.venue.name}
                    posterBg={heroEvent.posterBg}
                    height={320}
                  />
                </div>
              </div>
            </div>
          ) : null}
        </div>
      </section>

      {/* 2. PROMINENT SEARCH DOCK (Clean separation below Hero - never overlaps Hero buttons) */}
      <section className="tr-container mt-4">
        <div className="tr-search-dock p-3 p-md-4">
          <form onSubmit={handleSearchSubmit} className="row g-3 align-items-end">
            <div className="col-12 col-md-5">
              <label htmlFor="homeSearchInput" className="form-label small fw-semibold text-muted mb-1">
                Từ khoá sự kiện / Nghệ sĩ
              </label>
              <div className="input-group">
                <span className="input-group-text border-end-0">
                  <Search size={18} className="text-muted" />
                </span>
                <input
                  id="homeSearchInput"
                  type="text"
                  className="form-control border-start-0"
                  placeholder="Tìm tên đêm nhạc, lễ hội..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                />
              </div>
            </div>

            <div className="col-12 col-sm-6 col-md-4">
              <label htmlFor="homeCitySelect" className="form-label small fw-semibold text-muted mb-1">
                Khu vực / Thành phố
              </label>
              <div className="input-group">
                <span className="input-group-text border-end-0">
                  <MapPin size={18} className="text-muted" />
                </span>
                <select
                  id="homeCitySelect"
                  className="form-select border-start-0"
                  value={selectedCity}
                  onChange={(e) => setSelectedCity(e.target.value)}
                >
                  {CITIES.map(c => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="col-12 col-sm-6 col-md-3">
              <button type="submit" className="btn btn-tr-primary w-100 py-2">
                <Search size={18} />
                <span>Tìm vé ngay</span>
              </button>
            </div>
          </form>
        </div>
      </section>

      {/* 3. WHAT BUYERS GET */}
      <section className="tr-container mt-4">
        <div className="tr-promises">
          {PROMISES.map(item => (
            <div key={item.title} className="tr-promise">
              <span className="tr-promise-icon">{item.icon}</span>
              <div>
                <div className="fw-semibold text-main">{item.title}</div>
                <div className="small text-muted">{item.text}</div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 4. CATEGORY FILTER PILLS */}
      <section className="tr-container pt-4 pb-2">
        <div className="d-flex align-items-center justify-content-between mb-3">
          <h2 className="fs-5 fw-bold mb-0">Khám phá theo thể loại</h2>
          <Link to="/events" className="small fw-semibold text-primary text-decoration-none d-inline-flex align-items-center gap-1 text-nowrap flex-shrink-0">
            Bộ lọc nâng cao <ArrowRight size={15} />
          </Link>
        </div>
        <div className="d-flex align-items-center gap-2 overflow-auto pb-2" role="tablist" aria-label="Danh mục sự kiện">
          {CATEGORIES.map((cat) => {
            const isActive = activeCategory === cat.value;
            return (
              <button
                key={cat.value}
                role="tab"
                aria-selected={isActive}
                onClick={() => setActiveCategory(cat.value)}
                className={`btn btn-touch-target rounded-pill px-3 py-2 d-inline-flex align-items-center gap-2 flex-shrink-0 transition-all ${
                  isActive ? 'btn-tr-primary' : 'btn-tr-secondary'
                }`}
              >
                {cat.icon}
                <span>{cat.label}</span>
              </button>
            );
          })}
        </div>
      </section>

      {/* 5. MAIN EVENTS GRID */}
      <section className="tr-container py-4">
        <div className="d-flex align-items-center justify-content-between mb-4">
          <div>
            <h2 className="fs-4 fw-bold mb-1">Sự kiện nổi bật sắp diễn ra</h2>
            <p className="text-muted small mb-0">Đặt vé chính thức trực tiếp từ Ban Tổ Chức với giá niêm yết minh bạch</p>
          </div>
          <Link to="/events" className="btn-tr-ghost small text-primary text-decoration-none text-nowrap flex-shrink-0">
            <span>Xem tất cả ({events.length})</span>
            <ArrowRight size={16} />
          </Link>
        </div>

        {error && <ErrorState message={error} onRetry={fetchHomeData} />}

        {loading ? (
          <div className="row g-4">
            {[1, 2, 3, 4, 5, 6].map((n) => (
              <div key={n} className="col-12 col-md-6 col-lg-4">
                <EventCardSkeleton />
              </div>
            ))}
          </div>
        ) : filteredEvents.length === 0 ? (
          <EmptyState
            title="Chưa có sự kiện thuộc danh mục này"
            description="Hãy thử chọn danh mục khác hoặc xem toàn bộ các sự kiện đang mở bán trên hệ thống."
            actionText="Xem tất cả sự kiện"
            onAction={() => setActiveCategory('ALL')}
          />
        ) : (
          <div className="row g-4">
            {filteredEvents.slice(0, 9).map((event) => {
              const totalAvail = event.ticketTypes.reduce((acc, t) => acc + t.availableQuantity, 0);
              const soldOut = event.ticketTypes.length > 0 && totalAvail === 0;

              return (
                <div key={event.id} className="col-12 col-md-6 col-lg-4">
                  <Link
                    to={`/events/${event.slug}`}
                    className="tr-card tr-card-interactive overflow-hidden h-100 d-flex flex-column text-decoration-none"
                  >
                    <div className="position-relative">
                      <EventPoster
                        title={event.name}
                        category={event.category}
                        dateText={formatDateOnlyVN(event.startTime)}
                        city={event.venue.city}
                        posterBg={event.posterBg}
                        height={210}
                      />
                      {soldOut ? (
                        <span className="tr-poster-flag tr-poster-flag-muted">Hết vé</span>
                      ) : event.isHot ? (
                        <span className="tr-poster-flag">Sắp hết vé</span>
                      ) : null}
                    </div>

                    <div className="p-3 p-md-4 d-flex flex-column flex-grow-1 justify-content-between">
                      <div>
                        <div className="d-flex align-items-center gap-2 text-muted small mb-2">
                          <Calendar size={14} className="text-primary flex-shrink-0" />
                          <span className="tabular-nums fw-medium">{formatDateTimeVN(event.startTime)}</span>
                        </div>
                        <h3 className="fs-6 fw-bold text-main mb-2" style={{ lineHeight: 1.4 }}>
                          {event.name}
                        </h3>
                        <div className="d-flex align-items-center gap-1 text-muted small mb-3">
                          <MapPin size={14} className="flex-shrink-0" />
                          <span className="text-truncate">{event.venue.name}, {event.venue.city}</span>
                        </div>
                      </div>

                      <div className="pt-3 border-top border-subtle d-flex align-items-center justify-content-between mt-auto">
                        <div>
                          <span className="text-muted d-block" style={{ fontSize: '0.72rem' }}>Giá vé từ</span>
                          <span className="fw-bold fs-6 text-primary tabular-nums">
                            {formatVND(minTicketPrice(event))}
                          </span>
                        </div>
                        <span className="btn btn-sm btn-tr-secondary py-1 px-3">
                          Chọn vé
                        </span>
                      </div>
                    </div>
                  </Link>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* 6. EXPLORE BY CITY BENTO CARDS */}
      <section className="tr-container py-4">
        <div className="tr-card p-4 p-md-5 bg-surface-subtle">
          <div className="row align-items-center g-4">
            <div className="col-12 col-lg-4">
              <span className="tr-badge badge-info mb-2">ĐIỂM ĐẾN ÂM NHẠC</span>
              <h2 className="fs-4 fw-bold mb-2">Tâm điểm sự kiện tại 3 đô thị lớn</h2>
              <p className="text-muted small mb-0">
                Từ khán phòng giao hưởng cổ điển tại Hà Nội, đại nhạc hội ven biển Đà Nẵng đến những đêm nhạc Indie sân thượng tại TP. Hồ Chí Minh.
              </p>
            </div>
            <div className="col-12 col-lg-8">
              <div className="row g-3">
                {[
                  { city: 'Hà Nội', count: events.filter(e => e.venue.city === 'Hà Nội').length, desc: 'Nhà Hát Lớn • Cung Hữu Nghị • Mỹ Đình', accent: '#ea580c' },
                  { city: 'TP. Hồ Chí Minh', count: events.filter(e => e.venue.city === 'TP. Hồ Chí Minh').length, desc: 'SECC Quận 7 • Thảo Điền • Nhà Hát TP', accent: '#0d9488' },
                  { city: 'Đà Nẵng', count: events.filter(e => e.venue.city === 'Đà Nẵng').length, desc: 'Công Viên Biển Đông • Cung Tiên Sơn', accent: '#0284c7' },
                ].map((item) => (
                  <div key={item.city} className="col-12 col-sm-4">
                    <Link
                      to={`/events?city=${encodeURIComponent(item.city)}`}
                      className="tr-card tr-card-interactive p-3 d-block text-decoration-none h-100"
                      style={{ borderTop: `3px solid ${item.accent}` }}
                    >
                      <div className="d-flex justify-content-between align-items-center mb-1">
                        <span className="fw-bold text-main fs-6">{item.city}</span>
                        <span className="badge rounded-pill bg-surface-subtle text-primary border border-subtle tabular-nums">
                          {item.count} sự kiện
                        </span>
                      </div>
                      <p className="text-muted small mb-0" style={{ fontSize: '0.78rem' }}>{item.desc}</p>
                    </Link>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
