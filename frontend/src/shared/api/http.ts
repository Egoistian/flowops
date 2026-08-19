export type ApiProblem = {
  type?: string
  title?: string
  status: number
  detail: string
  instance?: string
  code: string
  traceId: string
  fieldErrors?: Array<{ field: string; message: string }>
}

let csrfReady = false

async function ensureCsrf() {
  if (csrfReady) return
  const response = await fetch('/api/session/csrf', { credentials: 'include' })
  if (!response.ok) throw await toProblem(response)
  await response.json()
  csrfReady = true
}

function csrfCookie() {
  return document.cookie
    .split('; ')
    .find((part) => part.startsWith('XSRF-TOKEN='))
    ?.split('=')[1]
}

async function toProblem(response: Response): Promise<ApiProblem> {
  try {
    return await response.json() as ApiProblem
  } catch {
    return {
      status: response.status,
      code: 'UNEXPECTED_RESPONSE',
      detail: '서버 응답을 읽을 수 없습니다.',
      traceId: response.headers.get('X-Trace-Id') ?? 'unavailable',
    }
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) await ensureCsrf()
  const csrf = csrfCookie()
  const response = await fetch(path, {
    ...init,
    credentials: 'include',
    headers: {
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...(csrf && !['GET', 'HEAD', 'OPTIONS'].includes(method)
        ? { 'X-XSRF-TOKEN': decodeURIComponent(csrf) }
        : {}),
      ...init.headers,
    },
  })
  if (!response.ok) throw await toProblem(response)
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export function resetCsrfState() {
  csrfReady = false
}
