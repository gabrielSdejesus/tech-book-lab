import type {
  Book,
  AiAssessmentRequest,
  AiAssessmentResponse,
  AiTestConnectionRequest,
  AiTestConnectionResponse,
  ProblemDetail,
  AiProviderInfo
} from '../types';

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

export async function assessWithAi(req: AiAssessmentRequest): Promise<AiAssessmentResponse> {
  const res = await fetch(`${API_BASE}/ai/assess`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
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
    },
    body: JSON.stringify(req)
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao testar conexão com o provedor de IA'));
  return res.json();
}

export async function getAiProviders(locale?: string): Promise<AiProviderInfo[]> {
  const activeLocale = locale || getActiveLocale();
  const res = await fetch(`${API_BASE}/ai/providers`, {
    headers: {
      'Accept-Language': activeLocale,
    },
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao consultar provedores de IA'));
  return res.json();
}

export async function saveChallengeSolution(challengeId: string, code: string, userReflection?: string): Promise<void> {
  const res = await fetch(`${API_BASE}/catalog/challenges/${challengeId}/solution`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ code, userReflection }),
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao salvar solução do desafio'));
}

export async function resetChallengeSolution(challengeId: string): Promise<void> {
  const res = await fetch(`${API_BASE}/catalog/challenges/${challengeId}/solution`, {
    method: 'DELETE',
  });
  if (!res.ok) throw new Error(await extractErrorMessage(res, 'Falha ao resetar solução do desafio'));
}
