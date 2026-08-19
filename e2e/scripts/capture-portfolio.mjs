import { chromium } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
import { resolve } from 'node:path'

const baseURL = 'http://127.0.0.1:4173'
const outputDir = resolve('../docs/portfolio/screenshots')
await mkdir(outputDir, { recursive: true })

const browser = await chromium.launch()
try {
  const desktop = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const desktopPage = await desktop.newPage()
  const desktopErrors = collectErrors(desktopPage)
  await desktopPage.goto(baseURL)
  await desktopPage.screenshot({ path: resolve(outputDir, 'flowops-login-desktop.png'), fullPage: true })
  await login(desktopPage)
  await createRequest(desktopPage)
  const requestUrl = desktopPage.url()
  await desktopPage.screenshot({ path: resolve(outputDir, 'flowops-request-desktop.png'), fullPage: true })
  assertNoErrors(desktopErrors)
  await desktop.close()

  const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 })
  const mobilePage = await mobile.newPage()
  const mobileErrors = collectErrors(mobilePage)
  await mobilePage.goto(baseURL)
  await login(mobilePage)
  await mobilePage.goto(requestUrl)
  await mobilePage.getByRole('heading', { name: '개발용 장비' }).waitFor()
  await mobilePage.screenshot({ path: resolve(outputDir, 'flowops-request-mobile.png'), fullPage: true })
  assertNoErrors(mobileErrors)
  await mobile.close()
} finally {
  await browser.close()
}

function collectErrors(page) {
  const errors = []
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(`console: ${message.text()}`)
  })
  page.on('pageerror', (error) => errors.push(`page: ${error.message}`))
  return errors
}

function assertNoErrors(errors) {
  if (errors.length) throw new Error(errors.join('\n'))
}

async function login(page) {
  await page.getByLabel('조직 키').fill('northstar')
  await page.getByLabel('이메일').fill('requester@northstar.example.com')
  await page.getByLabel('비밀번호').fill('demo-password')
  await page.getByRole('button', { name: '로그인' }).click()
  await page.getByRole('heading', { name: '새 요청을 구조화합니다.' }).waitFor()
}

async function createRequest(page) {
  await page.getByLabel('요청 제목').fill('개발용 장비')
  await page.getByLabel('사용 목적').fill('통합 테스트 환경 구성')
  await page.getByLabel('예산 코드').fill('ENG-2026')
  await page.getByLabel('품목명').fill('노트북')
  await page.getByLabel('수량').fill('2')
  await page.getByLabel('단가').fill('1200000')
  await page.getByRole('button', { name: '품목 추가' }).click()
  const itemNames = page.getByLabel('품목명')
  const quantities = page.getByLabel('수량')
  const prices = page.getByLabel('단가')
  await itemNames.nth(1).fill('도킹 스테이션')
  await quantities.nth(1).fill('1')
  await prices.nth(1).fill('180000')
  await page.getByRole('button', { name: '임시 저장' }).click()
  await page.getByRole('heading', { name: '개발용 장비' }).waitFor()
}
