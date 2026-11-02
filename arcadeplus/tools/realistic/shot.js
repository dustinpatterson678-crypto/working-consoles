const { chromium } = require('playwright-core');
(async () => {
  const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell', args: ['--use-gl=swiftshader','--enable-unsafe-swiftshader','--allow-file-access-from-files'] });
  const p = await b.newPage({ viewport: { width: 900, height: 1100 } });
  const [m, yaw, out, spin, icon] = process.argv.slice(2);
  await p.goto(`http://127.0.0.1:8765/preview.html?m=${m}&yaw=${yaw}&spin=${spin||0}&icon=${icon||0}`);
  await p.waitForFunction(() => document.title === 'done', null, { timeout: 60000 });
  if (icon == '1') { const el = await p.$('canvas'); await el.screenshot({ path: out, omitBackground: true }); } else await p.screenshot({ path: out }); await b.close();
})();
