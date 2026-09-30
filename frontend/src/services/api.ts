import type {
  Book,
  Lab,
  QueryResult,
  InfraStatus,
  AiAssessmentRequest,
  AiAssessmentResponse,
  AiTestConnectionRequest,
  AiTestConnectionResponse,
  EngineType,
  ProblemDetail
} from '../types';
import { getSessionId } from './session';

const API_BASE = '/api';

async function extractErrorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const contentType = res.headers.get('content-type') || '';
    if (contentType.includes('json') || contentType.includes('problem+json')) {
      const data: ProblemDetail = await res.json();
      if (data.errors && Array.isArray(data.errors) && data.errors.length > 0) {
        return data.errors.map((e) => `${e.field}: ${e.message}`).join(', ');
      }
      if (data.detail) return data.detail;
      if (data.title) return data.title;
      if (data.message) return data.message;
    }
  } catch {
    // fallback em caso de erro no parse
  }
  return fallback;
}

export async function getBooks(): Promise<Book[]> {
  const res = await fetch(`${API_BASE}/books`);
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao carregar catálogo de livros'));
  return res.json();
}

export async function getLabById(labId: string): Promise<Lab> {
  const res = await fetch(`${API_BASE}/labs/${labId}`);
  if (!res.ok) throw new Error(await extractErrorMessage(res, `Falha ao carregar laboratório ${labId}`));
  return res.json();
}

export async function executeQuery(query: string, engineType: EngineType, labId: string): Promise<QueryResult> {
  const res = await fetch(`${API_BASE}/query/execute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Session-Id': getSessionId(),
    },
    body: JSON.stringify({ query, engineType, labId })
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha na comunicação com o servidor de execução'));
  return res.json();
}

export async function resetLab(labId: string): Promise<QueryResult> {
  const res = await fetch(`${API_BASE}/query/reset/${labId}`, {
    method: 'POST',
    headers: {
      'X-Session-Id': getSessionId(),
    }
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao resetar banco do laboratório'));
  return res.json();
}

export async function getInfraStatus(): Promise<InfraStatus> {
  const res = await fetch(`${API_BASE}/infra/status`);
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao consultar status dos containers'));
  return res.json();
}

export async function assessWithAi(req: AiAssessmentRequest): Promise<AiAssessmentResponse> {
  const res = await fetch(`${API_BASE}/ai/assess`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Session-Id': getSessionId(),
    },
    body: JSON.stringify(req)
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao consultar Tutor de IA'));
  return res.json();
}

export async function testAiConnection(req: AiTestConnectionRequest): Promise<AiTestConnectionResponse> {
  const res = await fetch(`${API_BASE}/ai/test-connection`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Session-Id': getSessionId(),
    },
    body: JSON.stringify(req)
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao testar conexão com o provedor de IA'));
  return res.json();
}
