import { expect, test } from '@playwright/test'

test('creates, submits, and hides a procurement request across organizations', async ({ page }) => {
  await page.goto('/')
  await login(page, 'northstar', 'requester@northstar.example.com')

  await page.getByLabel('요청 제목').fill('개발용 장비')
  await page.getByLabel('사용 목적').fill('통합 테스트 환경')
  await page.getByLabel('예산 코드').fill('ENG-2026')
  await page.getByLabel('품목명').fill('노트북')
  await page.getByLabel('수량').fill('2')
  await page.getByLabel('단가').fill('1200000')
  await expect(page.getByText('2,400,000원')).toHaveCount(2)
  await page.getByRole('button', { name: '임시 저장' }).click()

  await expect(page).toHaveURL(/\/requests\/[0-9a-f-]+$/)
  const northstarRequestUrl = page.url()
  await expect(page.getByRole('heading', { name: '개발용 장비' })).toBeVisible()
  await page.getByRole('button', { name: '검토 요청 제출' }).click()
  await expect(page.getByText('SUBMITTED', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: '로그아웃' }).click()
  await page.goto('/')
  await login(page, 'acme', 'requester@acme.example.com')
  await page.goto(northstarRequestUrl)

  await expect(page.getByRole('heading', { name: '요청을 찾을 수 없습니다.' })).toBeVisible()
  await expect(page.getByText('개발용 장비')).toHaveCount(0)
})

async function login(page: import('@playwright/test').Page, organizationKey: string, email: string) {
  await page.getByLabel('조직 키').fill(organizationKey)
  await page.getByLabel('이메일').fill(email)
  await page.getByLabel('비밀번호').fill('demo-password')
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page.getByRole('heading', { name: '새 요청을 구조화합니다.' })).toBeVisible()
}
