import { useEffect, useState } from 'react'
import { Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { LoginPage } from '../features/auth/LoginPage'
import {
  ProcurementDetailPage,
  type ProcurementDetail,
} from '../features/procurement/ProcurementDetailPage'
import { ProcurementForm, type ProcurementDraftInput } from '../features/procurement/ProcurementForm'
import { flowopsApi, type ApiProblem } from '../features/procurement/procurementApi'
import { useSession } from './useSession'

export default function App() {
  const { session, loading, problem, login, logout } = useSession()
  if (loading && !session) return <div className="boot-screen">FLOWOPS / SESSION CHECK</div>
  if (!session) {
    return <LoginPage onLogin={login} busy={loading} error={problem?.detail} />
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand-lockup"><span className="brand-symbol">F/O</span><span>FLOWOPS</span></div>
        <div className="topbar__context">
          <span>NORTHSTAR / PROCUREMENT</span>
          <span className="online-mark">SYSTEM ONLINE</span>
        </div>
        <div className="user-block">
          <span>{session.displayName}</span>
          <span>{session.roles.join(' · ')}</span>
          <button className="text-button" type="button" onClick={() => void logout()}>로그아웃</button>
        </div>
      </header>
      <aside className="side-rail" aria-label="주요 메뉴">
        <a className="side-rail__item side-rail__item--active" href="/">구매 요청</a>
        <span className="side-rail__item side-rail__item--disabled">콘텐츠 승인</span>
        <span className="side-rail__item side-rail__item--disabled">현장 사건</span>
        <div className="side-rail__footer"><span>BUILD</span><strong>01</strong></div>
      </aside>
      <main className="workspace">
        <Routes>
          <Route path="/" element={<CreateRoute />} />
          <Route path="/requests/:requestId" element={<RequestRoute />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}

function CreateRoute() {
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [problem, setProblem] = useState<ApiProblem | null>(null)

  async function create(input: ProcurementDraftInput) {
    setBusy(true)
    setProblem(null)
    try {
      const result = await flowopsApi.createDraft(input)
      navigate(`/requests/${result.requestId}`)
    } catch (error) {
      setProblem(error as ApiProblem)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="workspace-grid">
      <section className="workspace-intro">
        <span className="eyebrow">WORK QUEUE / PROCUREMENT</span>
        <h1>새 요청을<br />구조화합니다.</h1>
        <p>금액은 브라우저에서 미리 계산하고 서버가 다시 검산합니다.</p>
        {problem && <p className="form-problem" role="alert">{problem.detail} · {problem.traceId}</p>}
      </section>
      <ProcurementForm onCreate={create} busy={busy} />
    </div>
  )
}

function RequestRoute() {
  const { requestId = '' } = useParams()
  const [request, setRequest] = useState<ProcurementDetail | null>(null)
  const [problem, setProblem] = useState<ApiProblem | null>(null)
  const [busy, setBusy] = useState(false)

  async function reload() {
    try {
      setRequest(await flowopsApi.getRequest(requestId))
      setProblem(null)
    } catch (error) {
      setProblem(error as ApiProblem)
    }
  }

  // Server state is intentionally synchronized when the route identifier changes.
  // oxlint-disable-next-line react-hooks/exhaustive-deps, react/set-state-in-effect
  useEffect(() => { void reload() }, [requestId])

  async function submit() {
    setBusy(true)
    try {
      await flowopsApi.submitRequest(requestId)
      await reload()
    } catch (error) {
      setProblem(error as ApiProblem)
    } finally {
      setBusy(false)
    }
  }

  if (!request && problem) {
    return <section className="empty-state"><span>{problem.code}</span><h1>요청을 찾을 수 없습니다.</h1><p>{problem.traceId}</p></section>
  }
  if (!request) return <div className="boot-screen">REQUEST / LOADING</div>
  return <ProcurementDetailPage request={request} problem={problem} onReload={reload} onSubmit={submit} busy={busy} />
}
