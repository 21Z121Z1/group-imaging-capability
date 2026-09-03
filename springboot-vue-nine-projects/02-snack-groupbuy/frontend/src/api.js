let token = '';

export const session = {
  get token() { return token; },
  set token(value) { token = typeof value === 'string' ? value : ''; }
};

export class ApiError extends Error {
  constructor(status, message, requestId = '') {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.requestId = requestId;
  }
}

export async function api(path, options = {}) {
  const controller = new AbortController();
  const timeoutMs = Number.isFinite(options.timeoutMs) ? options.timeoutMs : 15000;
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const headers = { Accept: 'application/json', ...(options.headers || {}) };
  const hasBody = options.body !== undefined && options.body !== null;
  if (hasBody && !(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  if (session.token) headers.Authorization = `Bearer ${session.token}`;

  try {
    const response = await fetch(path, {
      ...options,
      headers,
      credentials: 'same-origin',
      cache: 'no-store',
      signal: options.signal || controller.signal
    });
    if (response.status === 204) return null;

    const text = await response.text();
    let data = null;
    if (text) {
      try { data = JSON.parse(text); }
      catch { data = { message: text }; }
    }
    if (!response.ok) {
      const requestId = response.headers.get('X-Request-Id') || data?.requestId || '';
      throw new ApiError(response.status, data?.message || `请求失败（HTTP ${response.status}）`, requestId);
    }
    return data;
  } catch (error) {
    if (error?.name === 'AbortError') throw new ApiError(0, '请求超时，请检查网络后重试');
    if (error instanceof ApiError) throw error;
    throw new ApiError(0, '网络请求失败，请稍后重试');
  } finally {
    clearTimeout(timer);
  }
}
