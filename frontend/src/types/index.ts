export interface Challenge {
  id: string;
  order: number;
  title: string;
  description: string;
  scenario: string;
  starterTemplate: string;
  savedCode?: string | null;
  savedReflection?: string | null;
  guidelines: string[];
  reflectionPrompt: string;
  expectedReflection?: string;
}

export interface Lab {
  id: string;
  number: number;
  slug: string;
  title: string;
  summary: string;
  keyConcepts: string[];
  databaseName?: string;
  resetSchemaSql?: string;
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
  bannerText?: string;
  category?: string;
  edition?: string;
}

export interface AiAssessmentResponse {
  status: 'APPROVED' | 'NEEDS_REVISION' | 'DISCUSSION';
  feedback: string;
  tradeOffAnalysis: string;
  efficiencyNotes: string;
  alternativeApproaches: string[];
  modelUsed: string;
  expectedReflection?: string;
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

export interface AiModelInfo {
  id: string;
  name: string;
  recommended: boolean;
}

export interface AiProviderInfo {
  id: string;
  name: string;
  description: string;
  requiresApiKey: boolean;
  apiKeyPlaceholder?: string | null;
  helpUrl?: string | null;
  defaultModel: string;
  models: AiModelInfo[];
}

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type ScenarioType = 'CANVAS_MODELING' | 'SLIDER_SIMULATION';

export interface InteractiveScenario {
  id: string;
  labId: string;
  scenarioType: ScenarioType;
  difficulty: Difficulty;
  orderIndex: number;
  title: string;
  description: string;
  contextBriefing?: string;
  expectedReflection?: string;
  constraintsJson?: string;
  initialStateJson?: string;
  validationRulesJson?: string;
  savedUserStateJson?: string;
  savedUserReflection?: string;
  status?: string;
}

export interface ScenarioSolutionRequest {
  userStateJson: string;
  userReflection?: string;
}

export interface QuizOption {
  id: string;
  questionId: string;
  optionLetter: string;
  text: string;
  isCorrect?: boolean;
}

export interface QuizQuestion {
  id: string;
  chapterId: string;
  orderIndex: number;
  difficulty: Difficulty;
  prompt: string;
  explanation?: string;
  options: QuizOption[];
  selectedOptionId?: string;
  isAnsweredCorrectly?: boolean;
}

export interface ChapterQuiz {
  chapterId: string;
  questions: QuizQuestion[];
  totalQuestions: number;
  totalAnswered: number;
  totalCorrect: number;
}

export interface QuizSubmissionRequest {
  answers: Record<string, string>;
}

export interface QuizQuestionResult {
  questionId: string;
  selectedOptionId?: string;
  correctOptionId?: string;
  isCorrect: boolean;
  explanation?: string;
}

export interface QuizSubmissionResponse {
  chapterId: string;
  totalQuestions: number;
  correctAnswers: number;
  scorePercentage: number;
  results: QuizQuestionResult[];
}

export interface StressWorkload {
  concurrency: number;
  targetRps: number;
  durationSeconds: number;
  readRatio: number;
  keyDistribution: 'UNIFORM' | 'ZIPFIAN' | 'HOTSPOT';
  slaLatencyMs: number;
}

export interface StressTestRequest {
  modelJson: string;
  workload: StressWorkload;
}

export interface StressTestResponse {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  actualThroughputRps: number;
  p50LatencyMs: number;
  p99LatencyMs: number;
  lockContentionIndex: number;
  slaMet: boolean;
  diagnostics: string[];
  nodeBottlenecks: Record<string, string>;
}

export interface ScenarioAssessmentRequest {
  architectureJson: string;
  userReflection: string;
  stressMetrics?: StressTestResponse;
  provider?: string;
  model?: string;
}

