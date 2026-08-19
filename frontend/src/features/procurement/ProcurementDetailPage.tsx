import { useState } from 'react'

export type ApiProblem = {
  status: number
  code: string
  detail: string
  traceId: string
  fieldErrors?: Array<{ field: string; message: string }>
}

export type ProcurementDetail = {
  id: string
  title: string
  purpose: string
  budgetCode: string
  totalAmountKrw: number
  status: string
  version: number
  items: Array<{
    name: string
    quantity: number
    unitPriceKrw: number
    subtotalKrw: number
  }>
}

type ProcurementDetailPageProps = {
  request: ProcurementDetail
  problem?: ApiProblem | null
  onReload: () => void | Promise<void>
  onSubmit?: () => void | Promise<void>
  busy?: boolean
}

const numberFormatter = new Intl.NumberFormat('ko-KR')

export function ProcurementDetailPage({
  request,
  problem,
  onReload,
  onSubmit,
  busy = false,
}: ProcurementDetailPageProps) {
  const [reviewNote, setReviewNote] = useState('')
  const conflict = problem?.code === 'REQUEST_VERSION_CONFLICT'

  return (
    <article className="request-sheet">
      <header className="request-sheet__header">
        <div>
          <span className="eyebrow">PROCUREMENT REQUEST / {request.id.slice(0, 8).toUpperCase()}</span>
          <h2>{request.title}</h2>
          <p>{request.purpose}</p>
        </div>
        <div className="status-stack">
          <span className={`status-chip status-chip--${request.status.toLowerCase()}`}>
            {request.status}
          </span>
          <span>VERSION {request.version}</span>
        </div>
      </header>

      {conflict && (
        <section className="conflict-notice" role="alert" aria-labelledby="conflict-title">
          <div>
            <span className="eyebrow">409 / VERSION CONFLICT</span>
            <h3 id="conflict-title">다른 사용자가 먼저 변경했습니다</h3>
            <p>작성 중인 메모는 유지됩니다. 최신 서버 내용을 확인한 뒤 다시 시도하세요.</p>
          </div>
          <button className="secondary-button" type="button" onClick={() => void onReload()}>
            최신 내용 다시 불러오기
          </button>
        </section>
      )}

      <dl className="request-metadata">
        <div><dt>예산 코드</dt><dd>{request.budgetCode}</dd></div>
        <div><dt>품목 수</dt><dd>{request.items.length}</dd></div>
        <div><dt>합계</dt><dd>{numberFormatter.format(request.totalAmountKrw)}원</dd></div>
      </dl>

      <div className="request-table-wrap">
        <table className="request-table">
          <thead>
            <tr><th>번호</th><th>품목</th><th>수량</th><th>단가</th><th>소계</th></tr>
          </thead>
          <tbody>
            {request.items.map((item, index) => (
              <tr key={`${item.name}-${index}`}>
                <td>{String(index + 1).padStart(2, '0')}</td>
                <td>{item.name}</td>
                <td>{item.quantity}</td>
                <td>{numberFormatter.format(item.unitPriceKrw)}원</td>
                <td>{numberFormatter.format(item.subtotalKrw)}원</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr><td colSpan={4}>서버 계산 합계</td><td>{numberFormatter.format(request.totalAmountKrw)}원</td></tr>
          </tfoot>
        </table>
      </div>

      <label className="review-note">
        <span>내 검토 메모</span>
        <textarea
          rows={3}
          value={reviewNote}
          onChange={(event) => setReviewNote(event.target.value)}
          placeholder="페이지를 새로 불러오기 전까지 이 브라우저에만 유지됩니다."
        />
      </label>

      <footer className="request-sheet__footer">
        <span>TRACE {problem?.traceId ?? 'NO ACTIVE ERROR'}</span>
        {request.status === 'DRAFT' && onSubmit && (
          <button className="primary-button" type="button" disabled={busy} onClick={() => void onSubmit()}>
            {busy ? '제출 중…' : '검토 요청 제출'}
          </button>
        )}
      </footer>
    </article>
  )
}
