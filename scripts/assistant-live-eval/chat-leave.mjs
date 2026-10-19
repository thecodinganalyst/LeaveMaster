// Stateful, write-capable evaluation: only run against the reset EVALUATION tenant.
export async function evaluateChatLeave({request, csrf, tenantId, baselineDate}) {
  const staffId = tenantId + '-EMP001';
  const date = new Date(baselineDate + 'T12:00:00Z');
  date.setUTCDate(date.getUTCDate() + 35);
  while ([0, 6].includes(date.getUTCDay())) date.setUTCDate(date.getUTCDate() + 1);
  const leaveDate = date.toISOString().slice(0, 10);
  const failures = [];
  const attempts = [];
  const readApplications = async () => {
    const response = await request('staff', '/leave-applications?staffId=' + encodeURIComponent(staffId), {headers:{Accept:'application/json'}});
    if (!response.ok) throw new Error('Cannot verify persisted leave: HTTP ' + response.status);
    const data = await response.json();
    if (!Array.isArray(data)) throw new Error('Leave application listing is not an array');
    return data;
  };
  const matching = rows => rows.filter(row => row.leaveDate === leaveDate && row.staff?.id === staffId && (row.leaveType?.name || '').toLowerCase().includes('annual') && row.leaveDuration === 'FULL');
  const chat = async (message, conversationId) => {
    const response = await request('staff', '/api/assistant/chat', {method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({message,conversationId})});
    const body = await response.json().catch(()=>({}));
    if (!response.ok) throw new Error('Chat HTTP ' + response.status + ': ' + String(body.error || 'unknown error'));
    return body;
  };
  const started = Date.now();
  try {
    const before = await readApplications();
    const baseline = matching(before);
    if (baseline.length) throw new Error('Reset fixture already contains target leave date');
    let reply = await chat('Apply one full day of annual leave on ' + leaveDate + '. Please prepare the application for my confirmation.', null);
    let pending = (reply.pendingActions || []).filter(x=>x.toolName==='applyForLeave');
    for (let turn=0; turn<4 && pending.length===0; turn++) {
      if (!reply.conversationId) throw new Error('Assistant did not return a conversation ID');
      reply = await chat(turn===0 ? 'Yes, annual leave for the full day on ' + leaveDate + '. Please prepare it for confirmation.' : 'Please prepare the annual leave application for ' + leaveDate + ' and ask me to confirm.', reply.conversationId);
      pending = (reply.pendingActions || []).filter(x=>x.toolName==='applyForLeave');
    }
    if (pending.length !== 1 || !pending[0].confirmationToken) throw new Error('Exactly one pending applyForLeave confirmation is required');
    if (matching(await readApplications()).length !== 0) throw new Error('Leave was created before explicit confirmation');
    const confirmation = await request('staff','/api/assistant/actions/confirm',{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({confirmationToken:pending[0].confirmationToken})});
    const confirmationBody = await confirmation.json().catch(()=>({}));
    if (!confirmation.ok || confirmationBody.toolName !== 'applyForLeave' || confirmationBody.replayed) throw new Error('Leave confirmation failed: HTTP ' + confirmation.status);
    const after = matching(await readApplications());
    if (after.length !== 1) throw new Error('Expected exactly one persisted full-day annual leave, found ' + after.length);
    const replay = await request('staff','/api/assistant/actions/confirm',{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({confirmationToken:pending[0].confirmationToken})});
    const afterReplay = matching(await readApplications());
    if (afterReplay.length !== 1) throw new Error('Confirmation replay created duplicate leave');
    attempts.push({attempt:1,passed:true,latencyMs:Date.now()-started,tools:['applyForLeave'],replayStatus:replay.status});
  } catch(error) {
    failures.push(String(error?.message || error));
    attempts.push({attempt:1,passed:false,latencyMs:Date.now()-started,failures});
  }
  return {id:'employee-chat-apply-annual-leave',category:'leave-application',persona:'staff',critical:true,passed:failures.length===0,attempts,responseExcerpt:'',leaveDate};
}
