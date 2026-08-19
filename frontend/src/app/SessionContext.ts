import { createContext } from 'react'
import type { LoginInput } from '../features/auth/LoginPage'
import type { ApiProblem, Session } from '../features/procurement/procurementApi'

export type SessionContextValue = {
  session: Session | null
  loading: boolean
  problem: ApiProblem | null
  login: (input: LoginInput) => Promise<void>
  logout: () => Promise<void>
}

export const SessionContext = createContext<SessionContextValue | null>(null)
