import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getBooks,
  executeQuery,
  resetLab,
  assessWithAi,
  testAiConnection
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



  it('executeQuery deve enviar POST com query, engineType e labId', async () => {
    const mockResult = { success: true, columns: ['id'], rows: [{ id: 1 }], rowCount: 1, executionTimeMs: 12 };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockResult
    } as Response);

    const result = await executeQuery('SELECT 1;', 'POSTGRES', 'ddia-cap-03-lab-01');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/query/execute', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ query: 'SELECT 1;', engineType: 'POSTGRES', labId: 'ddia-cap-03-lab-01' })
    }));
    expect(result).toEqual(mockResult);
  });

  it('resetLab deve enviar POST para a rota de reset do lab', async () => {
    const mockReset = { success: true, message: 'Reset executado' };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockReset
    } as Response);

    const result = await resetLab('ddia-cap-03-lab-01');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/query/reset/ddia-cap-03-lab-01', expect.objectContaining({
      method: 'POST'
    }));
    expect(result).toEqual(mockReset);
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

  it('executeQuery deve extrair detail de ProblemDetail (RFC 7807) quando status for 400', async () => {
    const problemDetail = {
      type: 'urn:problem:query-execution-error',
      title: 'Erro na execução da consulta',
      status: 400,
      detail: 'relation "tabela_fantasma" does not exist'
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 400,
      headers: { get: () => 'application/problem+json' },
      json: async () => problemDetail
    } as unknown as Response);

    await expect(executeQuery('SELECT * FROM tabela_fantasma;', 'POSTGRES', 'ddia-cap-03-lab-01'))
      .rejects.toThrow('relation "tabela_fantasma" does not exist');
  });

  it('executeQuery deve formatar lista de errors de ProblemDetail (RFC 7807) quando campos forem inválidos', async () => {
    const problemDetail = {
      type: 'urn:problem:validation-error',
      title: 'Erro de validação sintática',
      status: 400,
      detail: 'Um ou mais campos da requisição são inválidos.',
      errors: [
        { field: 'query', message: 'A consulta SQL/Cypher é obrigatória' },
        { field: 'engineType', message: 'O tipo de motor de banco é obrigatório' }
      ]
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 400,
      headers: { get: () => 'application/problem+json' },
      json: async () => problemDetail
    } as unknown as Response);

    await expect(executeQuery('', 'POSTGRES', 'ddia-cap-03-lab-01'))
      .rejects.toThrow('query: A consulta SQL/Cypher é obrigatória, engineType: O tipo de motor de banco é obrigatório');
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
});

