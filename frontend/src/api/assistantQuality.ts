import { apiFetch } from './http.ts';

export interface AssistantQualityRequestRow {
  correlationId: string; timestamp: string; actorRole?: string; intentCategory?: string; tools?: string;
  latencyMs?: number; provider?: string; model?: string; retries?: number; success: boolean; failureCategory?: string;
}
export interface AssistantQualityDashboard {
  from: string; to: string; requests: number; successful: number; failed: number; successRatePercent: number;
  averageLatencyMs: number; p95LatencyMs: number; retryCount: number;
  failuresByCategory: Record<string, number>; tools: Record<string, number>; providerModels: Record<string, number>;
  positiveFeedback: number; negativeFeedback: number; recentRequests: AssistantQualityRequestRow[];
}
export const getAssistantQualityDashboard = (params: {days:number; outcome?:string; failureCategory?:string; provider?:string; model?:string}) => {
  const search=new URLSearchParams({days:String(params.days)});
  for(const [key,value] of Object.entries(params)) if(key!=='days'&&value) search.set(key,String(value));
  return apiFetch<AssistantQualityDashboard>(`/platform/assistant-quality?${search}`);
};
