import { useEffect, useState } from 'react';
import { Alert, Card, Col, Descriptions, Empty, Row, Select, Space, Spin, Statistic, Table, Typography } from 'antd';
import { getAssistantQualityDashboard, type AssistantQualityDashboard } from '../../api/assistantQuality.ts';

const numberOrZero = (value: unknown) => typeof value === 'number' && Number.isFinite(value) ? value : 0;
const recordOrEmpty = (value: unknown): Record<string, number> =>
  value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, number> : {};

const normalizeDashboard = (value: AssistantQualityDashboard): AssistantQualityDashboard => ({
  ...value,
  requests: numberOrZero(value?.requests),
  successful: numberOrZero(value?.successful),
  failed: numberOrZero(value?.failed),
  successRatePercent: numberOrZero(value?.successRatePercent),
  averageLatencyMs: numberOrZero(value?.averageLatencyMs),
  p95LatencyMs: numberOrZero(value?.p95LatencyMs),
  retryCount: numberOrZero(value?.retryCount),
  positiveFeedback: numberOrZero(value?.positiveFeedback),
  negativeFeedback: numberOrZero(value?.negativeFeedback),
  failuresByCategory: recordOrEmpty(value?.failuresByCategory),
  tools: recordOrEmpty(value?.tools),
  providerModels: recordOrEmpty(value?.providerModels),
  recentRequests: Array.isArray(value?.recentRequests) ? value.recentRequests : [],
});

export const AssistantQualityPage = () => {
  const [days,setDays]=useState(7); const [outcome,setOutcome]=useState<string>();
  const [data,setData]=useState<AssistantQualityDashboard>(); const [error,setError]=useState<string>(); const [loading,setLoading]=useState(true);
  useEffect(()=>{
    let active=true;
    setLoading(true);setError(undefined);
    getAssistantQualityDashboard(outcome ? {days,outcome} : {days})
      .then(value=>{if(active)setData(normalizeDashboard(value));})
      .catch(e=>{if(active)setError(e instanceof Error?e.message:'Unable to load quality data');})
      .finally(()=>{if(active)setLoading(false);});
    return()=>{active=false;};
  },[days,outcome]);
  if(loading) return <Spin tip="Loading AskLeaveMaestro quality…" />;
  if(error) return <Alert type="error" showIcon message="Unable to load AskLeaveMaestro quality" description={error} />;
  if(!data) return <Empty description="No quality data available" />;
  const cards: Array<[string, string | number]>=[['Requests',data.requests],['Success rate',`${data.successRatePercent.toFixed(1)}%`],['Avg latency',`${Math.round(data.averageLatencyMs)} ms`],['P95 latency',`${data.p95LatencyMs} ms`],['Retries',data.retryCount],['Feedback',`${data.positiveFeedback} 👍 / ${data.negativeFeedback} 👎`]];
  return <Space direction="vertical" size="large" style={{width:'100%'}}>
    <div><Typography.Title level={2}>AskLeaveMaestro Quality</Typography.Title><Typography.Text type="secondary">Privacy-safe operational metrics. Prompts and employee payloads are not displayed.</Typography.Text></div>
    <Space wrap><Select aria-label="Date range" value={days} onChange={setDays} options={[{value:1,label:'Last 24 hours'},{value:7,label:'Last 7 days'},{value:30,label:'Last 30 days'},{value:90,label:'Last 90 days'}]} /><Select aria-label="Outcome" allowClear placeholder="All outcomes" value={outcome} onChange={setOutcome} options={[{value:'success',label:'Success'},{value:'failure',label:'Failure'}]} /></Space>
    {data.requests===0?<Empty description="No assistant requests in this period" />:<>
      <Row gutter={[12,12]}>{cards.map(([label,value])=><Col xs={12} md={8} xl={4} key={String(label)}><Card><Statistic title={label} value={value} /></Card></Col>)}</Row>
      <Row gutter={[12,12]}><Col xs={24} lg={8}><Card title="Failures"><Descriptions column={1} items={Object.entries(data.failuresByCategory).map(([k,v])=>({key:k,label:k,children:v}))}/></Card></Col><Col xs={24} lg={8}><Card title="Tools invoked"><Descriptions column={1} items={Object.entries(data.tools).map(([k,v])=>({key:k,label:k,children:v}))}/></Card></Col><Col xs={24} lg={8}><Card title="Provider / model"><Descriptions column={1} items={Object.entries(data.providerModels).map(([k,v])=>({key:k,label:k,children:v}))}/></Card></Col></Row>
      <Card title="Recent requests"><Table rowKey={(r)=>r.correlationId+'-'+r.timestamp} scroll={{x:900}} pagination={{pageSize:20}} dataSource={data.recentRequests} columns={[
        {title:'Correlation ID',dataIndex:'correlationId'},{title:'Time',dataIndex:'timestamp',render:(v:string)=>v?new Date(v).toLocaleString():'—'},
        {title:'Role',dataIndex:'actorRole'},{title:'Intent',dataIndex:'intentCategory'},{title:'Tools',dataIndex:'tools'},
        {title:'Latency',dataIndex:'latencyMs',render:(v?:number)=>v==null?'—':`${v} ms`},{title:'Provider',dataIndex:'provider'},{title:'Model',dataIndex:'model'},
        {title:'Retries',dataIndex:'retries'},{title:'Outcome',dataIndex:'success',render:(v:boolean)=>v?'Success':'Failure'},{title:'Failure',dataIndex:'failureCategory'}
      ]}/></Card>
    </>}
  </Space>;
};
