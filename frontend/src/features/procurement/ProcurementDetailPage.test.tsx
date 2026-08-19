import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ProcurementDetailPage } from './ProcurementDetailPage'

describe('ProcurementDetailPage', () => {
  it('keeps an unsent review note while reloading after a version conflict', async () => {
    const user = userEvent.setup()
    const onReload = vi.fn()
    render(
      <ProcurementDetailPage
        request={{
          id: '11111111-aaaa-bbbb-cccc-111111111111',
          title: '개발용 장비',
          purpose: '통합 테스트 환경',
          budgetCode: 'ENG-2026',
          totalAmountKrw: 2_580_000,
          status: 'SUBMITTED',
          version: 1,
          items: [
            { name: '노트북', quantity: 2, unitPriceKrw: 1_200_000, subtotalKrw: 2_400_000 },
            { name: '도킹 스테이션', quantity: 1, unitPriceKrw: 180_000, subtotalKrw: 180_000 },
          ],
        }}
        problem={{
          status: 409,
          code: 'REQUEST_VERSION_CONFLICT',
          detail: 'Another user changed this request.',
          traceId: 'trace-example-001',
        }}
        onReload={onReload}
      />,
    )

    await user.type(screen.getByLabelText('내 검토 메모'), '예산 확인 필요')
    expect(screen.getByRole('alert')).toHaveTextContent('다른 사용자가 먼저 변경했습니다')
    await user.click(screen.getByRole('button', { name: '최신 내용 다시 불러오기' }))

    expect(onReload).toHaveBeenCalledOnce()
    expect(screen.getByLabelText('내 검토 메모')).toHaveValue('예산 확인 필요')
  })
})
