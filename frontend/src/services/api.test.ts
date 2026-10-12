import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getBooks,
  assessWithAi,
  testAiConnection,
  getAiProviders,
  saveChallengeSolution,
  resetChallengeSolution
} from './api';

describe('API Service', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('getBooks deve realizar GET em /api/books e retornar array de livros', async () => {
    const mockBooks = [{ id: 'ddia', title: 'Designing Data-Intensive Applications', chapters: [] }];
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockBooks
    } as Response);

    const result = await getBooks();

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/books', expect.objectContaining({
      headers: expect.objectContaining({
        'Accept-Language': 'pt'
      })
    }));
    expect(result).toEqual(mockBooks);
  });

  it('getBooks deve lançar erro quando a resposta HTTP for diferente de ok', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 500
    } as Response);

    await expect(getBooks()).rejects.toThrow('Falha ao carregar catálogo de livros');
  });

  it('getBooks deve enviar cabeçalho Accept-Language baseado no parâmetro ou localStorage', async () => {
    localStorage.setItem('tbl_locale', 'en');
    const mockBooks = [{ id: 'ddia', title: 'Designing Data-Intensive Applications', chapters: [] }];
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockBooks
    } as Response);

    await getBooks('en');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/books', expect.objectContaining({
      headers: expect.objectContaining({
        'Accept-Language': 'en'
      })
    }));
  });

  it('assessWithAi deve postar requisição de avaliação para /api/ai/assess', async () => {
    const mockAssessment = { status: 'APPROVED', feedback: 'Excelente!', architecturalAnalysis: 'Bom', executionAnalysis: 'Correto' };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockAssessment
    } as Response);

    const payload = {
      labId: 'ddia-cap-03-lab-01',
      challengeId: 'lab-01-ch-1',
      userQuery: 'SELECT 1',
      executionSummary: 'Sucesso',
      userReflection: 'Trade-off'
    };

    const result = await assessWithAi(payload);

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/ai/assess', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(payload)
    }));
    expect(result).toEqual(mockAssessment);
  });

  it('testAiConnection deve postar para /api/ai/test-connection', async () => {
    const mockConnection = { valid: true, message: 'OK', model: 'gemini-3.8-flash', latencyMs: 50 };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockConnection
    } as Response);

    const payload = { provider: 'gemini', apiKey: 'test-key' };
    const result = await testAiConnection(payload);

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/ai/test-connection', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(payload)
    }));
    expect(result).toEqual(mockConnection);
  });

  it('assessWithAi deve extrair detail de ProblemDetail em 400', async () => {
    const problemDetail = {
      type: 'urn:problem:domain-validation-error',
      title: 'Regra de negócio violada',
      status: 400,
      detail: 'Laboratório não encontrado com id: lab-fantasma'
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 400,
      headers: { get: () => 'application/problem+json' },
      json: async () => problemDetail
    } as unknown as Response);

    await expect(assessWithAi({
      labId: 'lab-fantasma',
      challengeId: 'ch-1',
      userQuery: 'SELECT 1;',
      executionSummary: '',
      userReflection: ''
    })).rejects.toThrow('Laboratório não encontrado com id: lab-fantasma');
  });

  it('testAiConnection deve extrair detail de ProblemDetail em 400', async () => {
    const problemDetail = {
      type: 'urn:problem:domain-validation-error',
      title: 'Regra de negócio violada',
      status: 400,
      detail: 'Provedor de IA não suportado: chatgpt. Provedores suportados: gemini, ollama'
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 400,
      headers: { get: () => 'application/problem+json' },
      json: async () => problemDetail
    } as unknown as Response);

    await expect(testAiConnection({ provider: 'chatgpt' }))
      .rejects.toThrow('Provedor de IA não suportado: chatgpt. Provedores suportados: gemini, ollama');
  });

  it('getAiProviders deve realizar GET em /api/ai/providers com cabeçalho Accept-Language', async () => {
    const mockProviders = [
      { id: 'gemini', name: 'Google Gemini', description: 'desc', requiresApiKey: true, defaultModel: 'gemini-2.5-flash', models: [] }
    ];
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockProviders
    } as Response);

    const result = await getAiProviders('en');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/ai/providers', expect.objectContaining({
      headers: expect.objectContaining({
        'Accept-Language': 'en'
      })
    }));
    expect(result).toEqual(mockProviders);
  });

  it('saveChallengeSolution deve realizar PUT em /api/catalog/challenges/:id/solution com body JSON', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({})
    } as Response);

    await saveChallengeSolution('lab-01-ch-1', 'SELECT 1;');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/catalog/challenges/lab-01-ch-1/solution', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({
        'Content-Type': 'application/json'
      }),
      body: JSON.stringify({ code: 'SELECT 1;', userReflection: undefined })
    }));
  });

  it('saveChallengeSolution deve incluir userReflection quando informado', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({})
    } as Response);

    await saveChallengeSolution('lab-01-ch-1', 'SELECT 1;', 'Reflexão sobre 3NF');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/catalog/challenges/lab-01-ch-1/solution', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({
        'Content-Type': 'application/json'
      }),
      body: JSON.stringify({ code: 'SELECT 1;', userReflection: 'Reflexão sobre 3NF' })
    }));
  });

  it('resetChallengeSolution deve realizar DELETE em /api/catalog/challenges/:id/solution', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({})
    } as Response);

    await resetChallengeSolution('lab-01-ch-1');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/catalog/challenges/lab-01-ch-1/solution', expect.objectContaining({
      method: 'DELETE'
    }));
  });

  describe('Cenários de Erro e Resiliência', () => {
    it('getBooks deve propagar exceção quando fetch falhar por erro de rede', async () => {
      vi.spyOn(globalThis, 'fetch').mockRejectedValueOnce(new TypeError('Failed to fetch'));

      await expect(getBooks()).rejects.toThrow('Failed to fetch');
    });

    it('assessWithAi deve propagar erro quando fetch rejeitar por queda de rede', async () => {
      vi.spyOn(globalThis, 'fetch').mockRejectedValueOnce(new TypeError('Connection refused'));

      await expect(assessWithAi({
        labId: 'ddia-cap-03-lab-01',
        challengeId: 'ch-1',
        userQuery: 'SELECT 1;',
        executionSummary: '',
        userReflection: ''
      })).rejects.toThrow('Connection refused');
    });

    it('assessWithAi deve usar mensagem de fallback padrão quando HTTP 500 ocorrer sem JSON estruturado', async () => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/html' },
        json: async () => { throw new Error('not json'); }
      } as unknown as Response);

      await expect(assessWithAi({
        labId: 'ddia-cap-03-lab-01',
        challengeId: 'ch-1',
        userQuery: 'SELECT 1;',
        executionSummary: '',
        userReflection: ''
      })).rejects.toThrow('Falha ao consultar Tutor de IA');
    });

    it('saveChallengeSolution e resetChallengeSolution devem lançar fallback amigável quando falharem', async () => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);

      await expect(saveChallengeSolution('ch-1', 'SELECT 1;')).rejects.toThrow('Falha ao salvar solução do desafio');

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);

      await expect(resetChallengeSolution('ch-1')).rejects.toThrow('Falha ao resetar solução do desafio');
    });
  });
});
