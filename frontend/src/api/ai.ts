import { get, post } from './client'
import type { AiStatusResponse, DiscoverResponse } from '../types/api'

/**
 * AI API surface — mirrors AiController in backend/ai-service, reached
 * through the API Gateway at /ai/**.
 */

export function getAiStatus(): Promise<AiStatusResponse> {
  return get<AiStatusResponse>('/ai/status')
}

export function discoverTalent(query: string): Promise<DiscoverResponse> {
  return post<DiscoverResponse>('/ai/discover', { query })
}
