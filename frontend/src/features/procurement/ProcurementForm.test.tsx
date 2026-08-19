import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ProcurementForm } from './ProcurementForm'

describe('ProcurementForm', () => {
  it('calculates the total and submits only server-allowed input fields', async () => {
    const user = userEvent.setup()
    const onCreate = vi.fn()
    render(<ProcurementForm onCreate={onCreate} />)

    await user.type(screen.getByLabelText('요청 제목'), '개발용 장비')
    await user.type(screen.getByLabelText('사용 목적'), '테스트 환경 구성')
    await user.type(screen.getByLabelText('예산 코드'), 'ENG-2026')
    await user.type(screen.getByLabelText('품목명'), '노트북')
    await user.clear(screen.getByLabelText('수량'))
    await user.type(screen.getByLabelText('수량'), '2')
    await user.clear(screen.getByLabelText('단가'))
    await user.type(screen.getByLabelText('단가'), '1200000')

    expect(screen.getAllByText('2,400,000원')).toHaveLength(2)
    await user.click(screen.getByRole('button', { name: '임시 저장' }))

    expect(onCreate).toHaveBeenCalledWith({
      title: '개발용 장비',
      purpose: '테스트 환경 구성',
      budgetCode: 'ENG-2026',
      items: [{ name: '노트북', quantity: 2, unitPriceKrw: 1_200_000 }],
    })
    expect(onCreate.mock.calls[0][0]).not.toHaveProperty('totalAmount')
    expect(onCreate.mock.calls[0][0]).not.toHaveProperty('organizationId')
    expect(onCreate.mock.calls[0][0]).not.toHaveProperty('requesterId')
  })
})
