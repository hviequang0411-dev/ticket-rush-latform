import type { ApiErrorProblem } from '../types/models';

// Same origin as the gateway (Caddy in production, the Vite proxy in development): the session cookie and the
// XSRF-TOKEN cookie set by the gateway just work.

const MESSAGES: Record<number, { title: string; message: string }> = {
  400: { title: 'Yêu cầu không hợp lệ', message: 'Vui lòng kiểm tra lại lựa chọn của bạn.' },
  401: { title: 'Cần đăng nhập', message: 'Vui lòng đăng nhập để tiếp tục.' },
  403: { title: 'Không có quyền truy cập', message: 'Tài khoản của bạn không được phép thực hiện thao tác này.' },
  404: { title: 'Không tìm thấy', message: 'Nội dung không tồn tại hoặc đã bị gỡ.' },
  409: { title: 'Hết vé hoặc xung đột', message: 'Vé đã hết hoặc vừa có thay đổi từ hệ thống.' },
  429: { title: 'Thao tác quá nhanh', message: 'Vui lòng chờ ít giây trước khi thử lại.' },
  503: { title: 'Hệ thống đang bận', message: 'Một dịch vụ tạm thời không phản hồi. Thử lại an toàn, bạn sẽ không bị trừ tiền hai lần.' },
};

function csrfToken(): string | undefined {
  return document.cookie
    .split('; ')
    .find(cookie => cookie.startsWith('XSRF-TOKEN='))
    ?.split('=')[1];
}

async function toProblem(response: Response): Promise<ApiErrorProblem> {
  const known = MESSAGES[response.status] ?? { title: `Lỗi ${response.status}`, message: 'Đã có lỗi xảy ra, vui lòng thử lại.' };
  let detail = known.message;
  try {
    const body = await response.json();
    // Backend problems (RFC 7807) carry a technical detail; buyers get the friendly message first
    if (body?.detail && response.status !== 500) {
      detail = `${known.message} (${body.detail})`;
    }
  } catch {
    // Not JSON
  }
  const retryAfter = Number(response.headers.get('Retry-After'));
  return {
    status: response.status,
    title: known.title,
    detail,
    message: known.message,
    retryAfterSeconds: Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : undefined,
  };
}

export async function request<T>(method: string, path: string, body?: unknown, headers: Record<string, string> = {}): Promise<T> {
  const init: RequestInit = { method, credentials: 'same-origin', headers: { Accept: 'application/json', ...headers } };
  if (body !== undefined) {
    init.body = JSON.stringify(body);
    (init.headers as Record<string, string>)['Content-Type'] = 'application/json';
  }
  if (method !== 'GET') {
    const token = csrfToken();
    if (token) {
      (init.headers as Record<string, string>)['X-XSRF-TOKEN'] = decodeURIComponent(token);
    }
  }
  let response: Response;
  try {
    response = await fetch(path, init);
  } catch {
    throw { status: 0, title: 'Mất kết nối', detail: 'Không kết nối được tới máy chủ.', message: 'Kiểm tra kết nối mạng và thử lại.' } satisfies ApiErrorProblem;
  }
  if (!response.ok) {
    throw await toProblem(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const get = <T>(path: string) => request<T>('GET', path);

export const post = <T>(path: string, body?: unknown, headers?: Record<string, string>) => request<T>('POST', path, body, headers);
