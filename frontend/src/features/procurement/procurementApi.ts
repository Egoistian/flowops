import { api, resetCsrfState, type ApiProblem } from '../../shared/api/http'
import type { LoginInput } from '../auth/LoginPage'
import type { ProcurementDetail } from './ProcurementDetailPage'
import type { ProcurementDraftInput } from './ProcurementForm'

export type Session = {
  userId: string
  organizationId: string
  displayName: string
  roles: string[]
}

type DraftResult = {
  requestId: string
  status: string
  totalAmountKrw: number
  version: number
}

export const flowopsApi = {
  session: () => api<Session>('/api/session'),
  login: (input: LoginInput) => api<Session>('/api/session/login', {
    method: 'POST',
    body: JSON.stringify(input),
  }),
  logout: async () => {
    await api<void>('/api/session/logout', { method: 'POST' })
    resetCsrfState()
  },
  createDraft: (input: ProcurementDraftInput) => api<DraftResult>('/api/procurement/requests', {
    method: 'POST',
    body: JSON.stringify(input),
  }),
  getRequest: (requestId: string) => api<ProcurementDetail>(`/api/procurement/requests/${requestId}`),
  submitRequest: (requestId: string) => api<DraftResult>(`/api/procurement/requests/${requestId}/submit`, {
    method: 'POST',
    body: JSON.stringify({ idempotencyKey: crypto.randomUUID() }),
  }),
}

export type { ApiProblem }
