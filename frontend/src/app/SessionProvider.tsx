import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { flowopsApi, type ApiProblem, type Session } from '../features/procurement/procurementApi'
import { SessionContext, type SessionContextValue } from './SessionContext'

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const [loading, setLoading] = useState(true)
  const [problem, setProblem] = useState<ApiProblem | null>(null)

  useEffect(() => {
    flowopsApi.session()
      .then(setSession)
      .catch(() => setSession(null))
      .finally(() => setLoading(false))
  }, [])

  const value = useMemo<SessionContextValue>(() => ({
    session,
    loading,
    problem,
    login: async (input) => {
      setLoading(true)
      setProblem(null)
      try {
        setSession(await flowopsApi.login(input))
      } catch (error) {
        setProblem(error as ApiProblem)
      } finally {
        setLoading(false)
      }
    },
    logout: async () => {
      await flowopsApi.logout()
      setSession(null)
    },
  }), [session, loading, problem])

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}
