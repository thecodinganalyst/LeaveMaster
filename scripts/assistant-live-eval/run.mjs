import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = (process.env.LIVE_EVAL_BASE_URL || '').replace(/\/$/, '');
const scenariosPath = process.env.LIVE_EVAL_SCENARIOS || 'scripts/assistant-live-eval/scenarios.json';
const reportDir = process.env.LIVE_EVAL_REPORT_DIR || 'build/reports/assistant-live-evaluation';
const maxAttempts = Number(process.env.LIVE_EVAL_MAX_ATTEMPTS || '2');
if (!baseUrl) throw new Error('LIVE_EVAL_BASE_URL is required');

const scenarios = JSON.parse(await fs.readFile(scenariosPath, 'utf8'));
const cookies = new Map();

function cookieHeader(persona) { return cookies.get(persona) || ''; }
function rememberCookie(persona, response) {
  const raw = response.headers.get('set-cookie');
  if (raw) cookies.set(persona, raw.split(';', 1)[0]);
}
async function request(persona, url, options = {}) {
  const headers = new Headers(options.headers || {});
  if (cookieHeader(persona)) headers.set('Cookie', cookieHeader(persona));
  const response = await fetch(baseUrl + url, {...options, headers, redirect:'manual'});
  rememberCookie(persona, response);
  return response;
}
async function login(persona) {
  const csrf = await request(persona, '/auth/csrf', {headers:{Accept:'application/json'}});
  if (!csrf.ok) throw new Error(`CSRF failed for ${persona}: HTTP ${csrf.status}`);
  const token = await csrf.json();
  const login = await request(persona, '/auth/demo-login', {method:'POST',headers:{'Content-Type':'application/json',[token.headerName]:token.token},body:JSON.stringify({persona})});
  if (!login.ok) throw new Error(`Demo login failed for ${persona}: HTTP ${login.status}`);
  return token;
}
const csrfByPersona = new Map();
async function evaluate(scenario) {
  if (!csrfByPersona.has(scenario.persona)) csrfByPersona.set(scenario.persona, await login(scenario.persona));
  const csrf = csrfByPersona.get(scenario.persona);
  const attempts = [];
  for (let attempt=1; attempt<=maxAttempts; attempt++) {
    const started=Date.now();
    try {
      const response=await request(scenario.persona,'/api/assistant/chat',{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({message:scenario.prompt,conversationId:null})});
      const body=await response.json().catch(()=>({}));
      const message=typeof body.message==='string'?body.message:'';
      const tools=Array.isArray(body.structuredResults)?body.structuredResults.map(x=>x.toolName).filter(Boolean):[];
      const failures=[];
      if(!response.ok) failures.push(`HTTP ${response.status}`);
      if(scenario.requiredAny?.length && !scenario.requiredAny.some(x=>message.toLowerCase().includes(x.toLowerCase()))) failures.push(`missing one of required facts: ${scenario.requiredAny.join(', ')}`);
      for(const forbidden of scenario.forbidden||[]) if(message.toLowerCase().includes(forbidden.toLowerCase())) failures.push(`contained forbidden fact: ${forbidden}`);
      for(const tool of scenario.expectedTools||[]) if(!tools.includes(tool)) failures.push(`expected tool not observed: ${tool}`);
      attempts.push({attempt,status:response.status,latencyMs:Date.now()-started,tools,passed:failures.length===0,failures});
      if(failures.length===0) return {...scenario,passed:true,attempts,responseExcerpt:message.slice(0,500)};
    } catch(error) {
      attempts.push({attempt,latencyMs:Date.now()-started,passed:false,failures:[String(error?.message||error)]});
    }
  }
  return {...scenario,passed:false,attempts,responseExcerpt:''};
}
const results=[];
for(const scenario of scenarios) results.push(await evaluate(scenario));
const passed=results.filter(x=>x.passed).length;
const criticalFailures=results.filter(x=>x.critical&&!x.passed);
const isolationFailures=results.filter(x=>x.category==='tenant-isolation'&&!x.passed);
const passRate=results.length?passed/results.length:0;
const gate={criticalPass:criticalFailures.length===0,tenantIsolationPass:isolationFailures.length===0,overallPass:passRate>=0.95,passRate};
const report={generatedAt:new Date().toISOString(),baseUrl,provider:'configured-deployment-provider',results,gate};
await fs.mkdir(reportDir,{recursive:true});
await fs.writeFile(path.join(reportDir,'live-evaluation.json'),JSON.stringify(report,null,2));
const rows=results.map(r=>`| ${r.id} | ${r.category} | ${r.critical?'yes':'no'} | ${r.passed?'PASS':'FAIL'} | ${r.attempts.length} | ${r.attempts.at(-1)?.latencyMs??''} | ${(r.attempts.at(-1)?.tools||[]).join(', ')} |`).join('\n');
const failures=results.filter(r=>!r.passed).map(r=>`### ${r.id}\n${r.attempts.map(a=>`- attempt ${a.attempt}: ${a.failures.join('; ')}`).join('\n')}`).join('\n\n');
const md=`# AskLeaveMaestro live-model evaluation\n\nGenerated: ${report.generatedAt}\n\nScenarios: ${results.length} | Passed: ${passed} | Pass rate: ${(passRate*100).toFixed(1)}%\n\nQuality gates: critical 100% **${gate.criticalPass?'PASS':'FAIL'}**; tenant isolation 100% **${gate.tenantIsolationPass?'PASS':'FAIL'}**; overall >=95% **${gate.overallPass?'PASS':'FAIL'}**.\n\nRetries are reported, never hidden. Response text is truncated in JSON and is not included in this Markdown report.\n\n| Scenario | Category | Critical | Result | Attempts | Last latency ms | Observed tools |\n|---|---|---:|---|---:|---:|---|\n${rows}\n\n${failures?'## Failures\n\n'+failures:''}\n`;
await fs.writeFile(path.join(reportDir,'live-evaluation.md'),md);
console.log(md);
if(!gate.criticalPass||!gate.tenantIsolationPass||!gate.overallPass) process.exitCode=1;
