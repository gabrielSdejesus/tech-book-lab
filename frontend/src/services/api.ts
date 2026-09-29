import type {
  Book,
  Lab,
  QueryResult,
  InfraStatus,
  AiAssessmentRequest,
  AiAssessmentResponse,
  AiTestConnectionRequest,
  AiTestConnectionResponse,
  EngineType
} from '../types';

const API_BASE = '/api';

export async function getBooks(): Promise<Book[]> {
  const res = await fetch(`${API_BASE}/books`);
  if (!res.ok) throw new Error('Falha ao carregar catálogo de livros');
  return res.json();
}

export async function getLabById(labId: string): Promise<Lab> {
  const res = await fetch(`${API_BASE}/labs/${labId}`);
  if (!res.ok) throw new Error(`Falha ao carregar laboratório ${labId}`);
  return res.json();
}

export async function executeQuery(query: string, engineType: EngineType, labId: string): Promise<QueryResult> {
  const res = await fetch(`${API_BASE}/query/execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, engineType, labId })
  });
  if (!res.ok) throw new Error('Falha na comunicação com o servidor de execução');
  return res.json();
}

export async function resetLab(labId: string): Promise<QueryResult> {
  const res = await fetch(`${API_BASE}/query/reset/${labId}`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error('Falha ao resetar banco do laboratório');
  return res.json();
}

export async function getInfraStatus(): Promise<InfraStatus> {
  const res = await fetch(`${API_BASE}/infra/status`);
  if (!res.ok) throw new Error('Falha ao consultar status dos containers');
  return res.json();
}

export async function assessWithAi(req: AiAssessmentRequest): Promise<AiAssessmentResponse> {
  const res = await fetch(`${API_BASE}/ai/assess`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req)
  });
  if (!res.ok) throw new Error('Falha ao consultar Tutor de IA');
  return res.json();
}

export async function testAiConnection(req: AiTestConnectionRequest): Promise<AiTestConnectionResponse> {
  const res = await fetch(`${API_BASE}/ai/test-connection`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req)
  });
  if (!res.ok) throw new Error('Falha ao testar conexão com o provedor de IA');
  return res.json();
}
