import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { LoginPage } from './LoginPage'

describe('LoginPage', () => {
  it('submits organization key, email, and password without storing auth state', async () => {
    const user = userEvent.setup()
    const onLogin = vi.fn()
    render(<LoginPage onLogin={onLogin} />)

    await user.type(screen.getByLabelText('조직 키'), 'northstar')
    await user.type(screen.getByLabelText('이메일'), 'requester@northstar.example.com')
    await user.type(screen.getByLabelText('비밀번호'), 'demo-password')
    await user.click(screen.getByRole('button', { name: '로그인' }))

    expect(onLogin).toHaveBeenCalledWith({
      organizationKey: 'northstar',
      email: 'requester@northstar.example.com',
      password: 'demo-password',
    })
  })
})
