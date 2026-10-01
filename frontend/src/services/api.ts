import type {
  Book,
  Lab,
  QueryResult,
  AiAssessmentRequest,
  AiAssessmentResponse,
  AiTestConnectionRequest,
  AiTestConnectionResponse,
  EngineType,
  ProblemDetail
} from '../types';
import { getSessionId } from './session';

const API_BASE = '/api';

export function getActiveLocale(): string {
  try {
    const saved = localStorage.getItem('tbl_locale');
    if (saved === 'en' || saved === 'pt') {
      return saved;
    }
  } catch {
    // fallback
  }
  return 'pt';
}

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

export async function getBooks(locale?: string): Promise<Book[]> {
  const activeLocale = locale || getActiveLocale();
  const res = await fetch(`${API_BASE}/books`, {
    headers: {
      'Accept-Language': activeLocale,
    },
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao carregar catálogo de livros'));
  return res.json();
}

export async function getBookById(bookId: string, locale?: string): Promise<Book> {
  const activeLocale = locale || getActiveLocale();
  const res = await fetch(`${API_BASE}/books/${encodeURIComponent(bookId)}`, {
    headers: {
      'Accept-Language': activeLocale,
    },
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao carregar livro'));
  return res.json();
}

export async function getLabById(labId: string, locale?: string): Promise<Lab> {
  const activeLocale = locale || getActiveLocale();
  const res = await fetch(`${API_BASE}/labs/${encodeURIComponent(labId)}`, {
    headers: {
      'Accept-Language': activeLocale,
    },
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao carregar laboratório'));
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
