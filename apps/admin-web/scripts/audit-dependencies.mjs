import {spawnSync} from 'node:child_process';
import {readFileSync} from 'node:fs';

// No patched release exists on 2026-10-11. This exception is restricted to
// development glob tooling and expires; production findings always fail.
const exception = 'https://github.com/advisories/GHSA-vfj7-8cjw-p6xm';
const expires = Date.parse('2026-11-11T00:00:00Z');
const lock = JSON.parse(readFileSync(new URL('../package-lock.json', import.meta.url)));
const reports = [];
for (const production of [true, false]) {
  const run = spawnSync('npm', ['audit', '--json', ...(production ? ['--omit=dev'] : [])],
    {encoding:'utf8', maxBuffer:8*1024*1024});
  if (run.error || run.signal || ![0,1].includes(run.status)) throw Error('Dependency audit could not complete.');
  const report = JSON.parse(run.stdout);
  if (report.error || !report.vulnerabilities || !report.metadata) throw Error('Invalid dependency audit response.');
  reports.push(report);
}
const serious = report => Object.values(report.vulnerabilities).filter(v => ['high','critical'].includes(v.severity));
if (serious(reports[0]).length) throw Error('Production dependency audit has high/critical findings. Run npm audit --omit=dev.');
const report = reports[1];
function allowed(v, seen = new Set()) {
  if (!v || seen.has(v.name) || !v.nodes?.length || !v.via?.length) return false;
  if (!v.nodes.every(path => lock.packages[path]?.dev === true)) return false;
  const next = new Set(seen).add(v.name);
  return v.via.every(cause => typeof cause === 'string'
    ? allowed(report.vulnerabilities[cause], next)
    : cause.url === exception && cause.name === 'braces' && cause.severity === 'high');
}
const findings = serious(report);
if (findings.some(v => Date.now() >= expires || !allowed(v)))
  throw Error('An unaccepted high/critical development advisory needs review. Run npm audit.');
console.log('Production high/critical audit passed.');
if (findings.length) console.log(`Accepted ${findings.length} development dependency findings from ${exception}; review before 2026-11-11. No upstream patch is available; do not evaluate untrusted glob patterns in build tooling.`);
else console.log('No high/critical development dependency findings.');
