export type EngineType = 'POSTGRES' | 'NEO4J';

export interface Challenge {
  id: string;
  order: number;
  title: string;
  description: string;
  scenario: string;
  starterTemplate: string;
  guidelines: string[];
  reflectionPrompt: string;
  engineType?: EngineType;
}

export interface Lab {
  id: string;
  number: number;
  slug: string;
  title: string;
  summary: string;
  keyConcepts: string[];
  engineType: EngineType;
  databaseName: string;
  resetSchemaSql: string;
  challenges: Challenge[];
}

export interface Chapter {
  id: string;
  number: number;
  title: string;
  subtitle: string;
  summary: string;
  labs: Lab[];
}

export interface Book {
  id: string;
  title: string;
  author: string;
  tagLine: string;
  coverColor: string;
  coverImageUrl?: string;
  description: string;
  chapters: Chapter[];
}

export interface QueryResult {
  success: boolean;
  message: string;
  columns: string[];
  rows: Record<string, any>[];
  rowCount: number;
  executionTimeMs: number;
  errorMessage: string | null;
}

export interface AiAssessmentResponse {
  status: 'APPROVED' | 'NEEDS_REVISION' | 'DISCUSSION';
  feedback: string;
  tradeOffAnalysis: string;
  efficiencyNotes: string;
  alternativeApproaches: string[];
  modelUsed: string;
}

export interface AiAssessmentRequest {
  labId: string;
  challengeId: string;
  userQuery: string;
  executionSummary: string;
  userReflection: string;
  apiKeyOverride?: string;
  providerOverride?: string;
  modelOverride?: string;
  language?: string;
}


export interface AiTestConnectionRequest {
  provider: string;
  apiKey?: string;
  modelOverride?: string;
}

export interface AiTestConnectionResponse {
  valid: boolean;
  message: string;
  model: string | null;
  latencyMs: number;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Array<{ field: string; message: string }>;
  [key: string]: any;
}

