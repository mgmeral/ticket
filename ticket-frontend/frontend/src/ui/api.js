function getBaseUrl(){
  const cfg = (typeof window !== 'undefined' && window.__CONFIG__) ? window.__CONFIG__ : null;
  return (cfg && cfg.API_BASE_URL) ? cfg.API_BASE_URL : '/api';
}

async function request(method, path, body){
  const base = getBaseUrl();
  const url = base.replace(/\/$/, '') + (path.startsWith('/') ? path : '/' + path);
  const res = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const contentType = res.headers.get('content-type') || '';
  const text = await res.text();
  const data = contentType.includes('application/json') && text ? JSON.parse(text) : text;

  if(!res.ok){
    const msg = typeof data === 'string' ? data : (data?.message || data?.error || JSON.stringify(data));
    const err = new Error(`${res.status} ${res.statusText}: ${msg}`);
    err.status = res.status;
    err.payload = data;
    throw err;
  }
  return data;
}

export const api = {
  get: (p) => request('GET', p),
  post: (p,b) => request('POST', p,b),
  put: (p,b) => request('PUT', p,b),
  del: (p) => request('DELETE', p),
  baseUrl: getBaseUrl,
};
