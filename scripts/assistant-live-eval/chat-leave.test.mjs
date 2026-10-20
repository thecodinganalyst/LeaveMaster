import test from 'node:test';
import assert from 'node:assert/strict';
import { evaluateChatLeave } from './chat-leave.mjs';

function fixture({premature=false, duplicate=false}={}) {
  const rows=[];
  let calls=0;
  let confirmations=0;
  const request=async (_persona,url,options={})=>{
    if(url.startsWith('/leave-applications?')) return {ok:true,json:async()=>rows};
    if(url==='/api/assistant/chat') {
      calls++;
      if(premature && calls===1) rows.push({leaveDate:'2026-11-23',staff:{id:'EVALUATION-EMP001'},leaveType:{name:'Annual Leave'},leaveDuration:'FULL'});
      return {ok:true,json:async()=>({conversationId:'conversation-1',pendingActions:[{toolName:'applyForLeave',confirmationToken:'token'}]})};
    }
    if(url==='/api/assistant/actions/confirm') {
      confirmations++;
      if(confirmations===1 || duplicate) rows.push({leaveDate:'2026-11-23',staff:{id:'EVALUATION-EMP001'},leaveType:{name:'Annual Leave'},leaveDuration:'FULL'});
      return {ok:true,status:200,json:async()=>({toolName:'applyForLeave',replayed:confirmations>1})};
    }
    throw new Error('Unexpected request: '+url);
  };
  return {request,csrf:{headerName:'X-CSRF-TOKEN',token:'test'},tenantId:'EVALUATION',baselineDate:'2026-10-19'};
}
test('chat leave evaluation confirms and verifies exactly one persisted request',async()=>{
  const result=await evaluateChatLeave(fixture());
  assert.equal(result.passed,true,JSON.stringify(result.attempts));
});
test('chat leave evaluation rejects submission before confirmation',async()=>{
  const result=await evaluateChatLeave(fixture({premature:true}));
  assert.equal(result.passed,false);
  assert.match(result.attempts[0].failures.join(' '),/before explicit confirmation/);
});
test('chat leave evaluation rejects duplicate confirmation side effects',async()=>{
  const result=await evaluateChatLeave(fixture({duplicate:true}));
  assert.equal(result.passed,false);
  assert.match(result.attempts[0].failures.join(' '),/duplicate/);
});

test('transient provider 502 is retried before confirmation without duplicate leave',async()=>{
  const base=fixture();
  let chatCalls=0;
  const request=async(persona,url,options)=>{
    if(url==='/api/assistant/chat' && ++chatCalls===1)
      return {ok:false,status:502,json:async()=>({error:'The AI provider could not complete the request'})};
    return base.request(persona,url,options);
  };
  const result=await evaluateChatLeave({...base,request});
  assert.equal(result.passed,true,JSON.stringify(result.attempts));
  assert.equal(chatCalls,2);
});
test('persistent provider 502 fails rather than bypassing quality gate',async()=>{
  const base=fixture();
  let chatCalls=0;
  const request=async(persona,url,options)=>{
    if(url==='/api/assistant/chat') {
      chatCalls++;
      return {ok:false,status:502,json:async()=>({error:'provider unavailable'})};
    }
    return base.request(persona,url,options);
  };
  const result=await evaluateChatLeave({...base,request});
  assert.equal(result.passed,false);
  assert.equal(chatCalls,2);
  assert.match(result.attempts[0].failures.join(' '),/Chat HTTP 502/);
});
test('failed chat with unexpected persisted leave refuses retry',async()=>{
  const base=fixture();
  const rows=[{leaveDate:'2026-11-23',staff:{id:'EVALUATION-EMP001'},leaveType:{name:'Annual Leave'},leaveDuration:'FULL'}];
  let chats=0;
  let reads=0;
  const request=async(persona,url,options)=>{
    if(url.startsWith('/leave-applications?')) return {ok:true,json:async()=>++reads===1?[]:rows};
    if(url==='/api/assistant/chat') {
      chats++;
      return {ok:false,status:502,json:async()=>({error:'provider unavailable'})};
    }
    return base.request(persona,url,options);
  };
  const result=await evaluateChatLeave({...base,request});
  assert.equal(result.passed,false);
  assert.equal(chats,1);
  assert.match(result.attempts[0].failures.join(' '),/refusing retry/);
});
