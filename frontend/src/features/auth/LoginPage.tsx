import { useForm } from 'react-hook-form'

export type LoginInput = {
  organizationKey: string
  email: string
  password: string
}

type LoginPageProps = {
  onLogin: (input: LoginInput) => void | Promise<void>
  busy?: boolean
  error?: string
}

export function LoginPage({ onLogin, busy = false, error }: LoginPageProps) {
  const { register, handleSubmit } = useForm<LoginInput>({
    defaultValues: { organizationKey: '', email: '', password: '' },
  })

  return (
    <main className="login-shell">
      <section className="login-manifest" aria-labelledby="manifest-title">
        <div className="brand-lockup">
          <span className="brand-symbol" aria-hidden="true">F/O</span>
          <span>FLOWOPS</span>
        </div>
        <div>
          <span className="eyebrow">OPERATIONAL APPROVAL SYSTEM / 2026</span>
          <h1 id="manifest-title">결정의 흐름을<br />증거로 남깁니다.</h1>
          <p>
            요청, 검토, 승인, 충돌을 하나의 기록으로 연결하는
            조직 범위 업무 운영 콘솔입니다.
          </p>
        </div>
        <dl className="manifest-facts">
          <div><dt>AUTH</dt><dd>SESSION + CSRF</dd></div>
          <div><dt>SCOPE</dt><dd>ORGANIZATION BOUND</dd></div>
          <div><dt>STATE</dt><dd>VERSION CONTROLLED</dd></div>
        </dl>
      </section>

      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-panel__topline">
          <span>SECURE ACCESS</span>
          <span>01 / 03</span>
        </div>
        <div className="login-panel__content">
          <span className="eyebrow">IDENTIFY YOUR WORKSPACE</span>
          <h2 id="login-title">조직 계정 로그인</h2>
          <p>자격증명은 브라우저 저장소에 보관되지 않습니다.</p>
          <form onSubmit={handleSubmit((values) => onLogin(values))}>
            <label>
              <span>조직 키</span>
              <input autoComplete="organization" {...register('organizationKey', { required: true })} />
            </label>
            <label>
              <span>이메일</span>
              <input type="email" autoComplete="username" {...register('email', { required: true })} />
            </label>
            <label>
              <span>비밀번호</span>
              <input type="password" autoComplete="current-password" {...register('password', { required: true })} />
            </label>
            {error && <p className="form-problem" role="alert">{error}</p>}
            <button className="primary-button primary-button--wide" type="submit" disabled={busy}>
              {busy ? '확인 중…' : '로그인'}
            </button>
          </form>
          <p className="demo-hint">
            데모 조직 <code>northstar</code> · 공개 예시 계정만 사용합니다.
          </p>
        </div>
      </section>
    </main>
  )
}
