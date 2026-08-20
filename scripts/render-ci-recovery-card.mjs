import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import playwright from '../e2e/node_modules/playwright/index.js';

const { chromium } = playwright;

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(scriptDirectory, '..');
const outputPath = resolve(projectRoot, 'docs/portfolio/screenshots/flowops-ci-recovery-1600x1200.png');

function fontData(relativePath) {
  return readFileSync(resolve(projectRoot, relativePath)).toString('base64');
}

const sansRegular = fontData('frontend/node_modules/@fontsource/ibm-plex-sans-kr/files/ibm-plex-sans-kr-latin-400-normal.woff2');
const sansSemibold = fontData('frontend/node_modules/@fontsource/ibm-plex-sans-kr/files/ibm-plex-sans-kr-latin-600-normal.woff2');
const monoRegular = fontData('frontend/node_modules/@fontsource/ibm-plex-mono/files/ibm-plex-mono-latin-400-normal.woff2');
const monoSemibold = fontData('frontend/node_modules/@fontsource/ibm-plex-mono/files/ibm-plex-mono-latin-600-normal.woff2');

const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<style>
  @font-face { font-family: PlexSans; src: url(data:font/woff2;base64,${sansRegular}) format('woff2'); font-weight: 400; }
  @font-face { font-family: PlexSans; src: url(data:font/woff2;base64,${sansSemibold}) format('woff2'); font-weight: 600; }
  @font-face { font-family: PlexMono; src: url(data:font/woff2;base64,${monoRegular}) format('woff2'); font-weight: 400; }
  @font-face { font-family: PlexMono; src: url(data:font/woff2;base64,${monoSemibold}) format('woff2'); font-weight: 600; }
  :root {
    --paper: #eee9df;
    --ink: #18211f;
    --muted: #68706b;
    --line: rgba(24, 33, 31, .22);
    --faint: rgba(24, 33, 31, .075);
    --oxide: #b24a38;
    --mineral: #16786e;
    --pale-red: #e7c8bf;
    --pale-green: #c9ddd8;
  }
  * { box-sizing: border-box; }
  html, body { width: 1600px; height: 1200px; margin: 0; overflow: hidden; }
  body {
    font-family: PlexSans, sans-serif;
    color: var(--ink);
    background:
      radial-gradient(circle at 1px 1px, rgba(24,33,31,.12) 1px, transparent 1.3px) 0 0 / 24px 24px,
      linear-gradient(90deg, transparent 79px, var(--faint) 80px, transparent 81px) 0 0 / 160px 100%,
      var(--paper);
  }
  main { position: relative; width: 100%; height: 100%; padding: 78px 84px 66px; }
  main::before {
    content: '';
    position: absolute;
    inset: 42px;
    border: 1px solid var(--line);
    pointer-events: none;
  }
  .topline { display: flex; justify-content: space-between; align-items: flex-start; }
  .eyebrow, .meta, .index, .micro, .metric-label, .run {
    font-family: PlexMono, monospace;
    text-transform: uppercase;
    letter-spacing: .12em;
  }
  .eyebrow { font-size: 17px; font-weight: 600; }
  .meta { text-align: right; font-size: 14px; line-height: 1.65; color: var(--muted); }
  h1 { margin: 62px 0 18px; font-size: 91px; line-height: .92; letter-spacing: -.058em; font-weight: 600; max-width: 1170px; }
  .dek { font-size: 24px; line-height: 1.45; max-width: 900px; color: #4e5853; }
  .timeline { position: relative; margin-top: 74px; display: grid; grid-template-columns: repeat(4, 1fr); border-top: 1px solid var(--ink); border-bottom: 1px solid var(--ink); }
  .timeline::before {
    content: '';
    position: absolute;
    left: 7%; right: 7%; top: 81px; height: 2px;
    background: linear-gradient(90deg, var(--mineral) 0 24%, var(--oxide) 24% 49%, var(--ink) 49% 73%, var(--mineral) 73% 100%);
  }
  .stage { position: relative; min-height: 338px; padding: 31px 28px 28px; border-right: 1px solid var(--line); }
  .stage:last-child { border-right: 0; }
  .index { font-size: 14px; color: var(--muted); }
  .node { position: absolute; top: 69px; left: 27px; width: 27px; height: 27px; border: 6px solid var(--paper); outline: 2px solid var(--ink); background: var(--paper); border-radius: 50%; z-index: 2; }
  .stage.fail .node { background: var(--oxide); outline-color: var(--oxide); }
  .stage.pass .node, .stage.recover .node { background: var(--mineral); outline-color: var(--mineral); }
  .stage h2 { margin: 96px 0 14px; font-size: 31px; line-height: 1.05; letter-spacing: -.025em; }
  .stage p { margin: 0; font-size: 17px; line-height: 1.52; color: #4d5651; max-width: 285px; }
  .code { display: inline-block; margin-top: 20px; padding: 10px 12px; font: 600 15px/1 PlexMono, monospace; border: 1px solid var(--line); background: rgba(255,255,255,.25); }
  .fail .code { color: var(--oxide); border-color: rgba(178,74,56,.5); background: rgba(231,200,191,.42); }
  .recover .code { color: var(--mineral); border-color: rgba(22,120,110,.42); background: rgba(201,221,216,.48); }
  .run { margin-top: 16px; font-size: 12px; color: var(--muted); }
  .metrics { display: grid; grid-template-columns: repeat(5, 1fr); margin-top: 40px; border: 1px solid var(--line); background: rgba(238,233,223,.75); }
  .metric { min-height: 116px; padding: 22px 24px; border-right: 1px solid var(--line); }
  .metric:last-child { border-right: 0; }
  .metric-value { font: 600 32px/1 PlexMono, monospace; }
  .metric-value.green { color: var(--mineral); }
  .metric-label { margin-top: 14px; font-size: 12px; line-height: 1.35; color: var(--muted); }
  .footer { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 20px; }
  .reflection { max-width: 1080px; font-size: 16px; line-height: 1.35; }
  .reflection strong { color: var(--oxide); font-weight: 600; }
  .stamp { text-align: right; }
  .stamp .micro { font-size: 12px; color: var(--muted); }
  .stamp .hash { margin-top: 7px; font: 600 18px/1 PlexMono, monospace; color: var(--mineral); }
</style>
</head>
<body>
<main>
  <div class="topline">
    <div class="eyebrow">FlowOps / Publication CI Incident / 2026</div>
    <div class="meta">Independent build evidence<br>macOS local → GitHub Ubuntu</div>
  </div>

  <h1>LOCAL GREEN<br>≠ REMOTE GREEN</h1>
  <div class="dek">A failed safety gate became an explicit portability contract—then passed twice on the public runner.</div>

  <section class="timeline">
    <article class="stage pass">
      <div class="index">01 / Local proof</div><div class="node"></div>
      <h2>FULL LOCAL PASS</h2>
      <p>Backend, frontend, Compose health, cross-organization browser E2E, and public-data scans.</p>
      <div class="code">PASS first-slice</div>
    </article>
    <article class="stage fail">
      <div class="index">02 / First push</div><div class="node"></div>
      <h2>REMOTE SAFETY FAIL</h2>
      <p>Application and E2E passed. The shell boundary failed before it could read rendered Compose.</p>
      <div class="code">command not found</div>
      <div class="run">RUN 32314882027</div>
    </article>
    <article class="stage">
      <div class="index">03 / Root cause</div><div class="node"></div>
      <h2>ONE TOOL, TWO SHAPES</h2>
      <p>Standalone on local macOS. Docker CLI plugin on the GitHub Ubuntu runner.</p>
      <div class="code">docker-compose ↔ docker compose</div>
    </article>
    <article class="stage recover">
      <div class="index">04 / Verified recovery</div><div class="node"></div>
      <h2>CONTRACT + REGRESSION</h2>
      <p>Plugin-first fallback, explicit failure when absent, and a controlled plugin-only PATH fixture.</p>
      <div class="code">REMOTE GREEN × 2</div>
      <div class="run">RUNS 32315315195 / 32315607695</div>
    </article>
  </section>

  <section class="metrics">
    <div class="metric"><div class="metric-value">28/28</div><div class="metric-label">BACKEND TESTS</div></div>
    <div class="metric"><div class="metric-value">3/3</div><div class="metric-label">FRONTEND TESTS</div></div>
    <div class="metric"><div class="metric-value">1/1</div><div class="metric-label">CHROMIUM E2E</div></div>
    <div class="metric"><div class="metric-value green">4 PASS</div><div class="metric-label">PORTABILITY · NETWORK · AUTH STORAGE · PUBLIC DATA</div></div>
    <div class="metric"><div class="metric-value green">SUCCESS</div><div class="metric-label">FINAL GITHUB ACTIONS</div></div>
  </section>

  <div class="footer">
    <div class="reflection"><strong>Reflection:</strong> the fix was not a renamed command. It was turning two environments into one tested compatibility boundary.</div>
    <div class="stamp"><div class="micro">Final public main</div><div class="hash">2297BCE</div></div>
  </div>
</main>
</body>
</html>`;

const browser = await chromium.launch({ headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1600, height: 1200 }, deviceScaleFactor: 1 });
  await page.setContent(html, { waitUntil: 'load' });
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: outputPath, type: 'png' });
} finally {
  await browser.close();
}

console.log(outputPath);
