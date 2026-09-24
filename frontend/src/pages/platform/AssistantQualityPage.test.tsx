import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { AssistantQualityPage } from './AssistantQualityPage.tsx';
import * as api from '../../api/assistantQuality.ts';
vi.mock('../../api/assistantQuality.ts');
test('renders privacy-safe AskLeaveMaestro quality metrics', async () => {
 vi.mocked(api.getAssistantQualityDashboard).mockResolvedValue({from:'2026-09-23T00:00:00Z',to:'2026-09-24T00:00:00Z',requests:2,successful:1,failed:1,successRatePercent:50,averageLatencyMs:200,p95LatencyMs:300,retryCount:1,failuresByCategory:{TOOL:1},tools:{getLeaveBalances:1},providerModels:{'gemini/model':2},positiveFeedback:1,negativeFeedback:0,recentRequests:[{correlationId:'c1',timestamp:'2026-09-24T00:00:00Z',actorRole:'STAFF',intentCategory:'LEAVE',tools:'getLeaveBalances',latencyMs:100,provider:'gemini',model:'model',retries:0,success:true}]});
 render(<AssistantQualityPage/>);
 expect(await screen.findByText('AskLeaveMaestro Quality')).toBeInTheDocument();
 expect(screen.getByText('50.0%')).toBeInTheDocument();
 expect(screen.getByText('c1')).toBeInTheDocument();
 expect(screen.queryByText(/prompt text|employee payload/i)).not.toBeInTheDocument();
});
test('renders an error state', async()=>{vi.mocked(api.getAssistantQualityDashboard).mockRejectedValue(new Error('failed'));render(<AssistantQualityPage/>);expect(await screen.findByText('Unable to load AskLeaveMaestro quality')).toBeInTheDocument();});
