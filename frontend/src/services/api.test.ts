import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getBooks,
  getLabById,
  executeQuery,
  resetLab,
  getInfraStatus,
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

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/books');
    expect(result).toEqual(mockBooks);
  });

  it('getBooks deve lançar erro quando a resposta HTTP for diferente de ok', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 500
    } as Response);

    await expect(getBooks()).rejects.toThrow('Falha ao carregar catálogo de livros');
  });

  it('getLabById deve buscar laboratório específico na URL correta', async () => {
    const mockLab = { id: 'ddia-cap-03-lab-01', title: 'Lab 1', engineType: 'POSTGRES' };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockLab
    } as Response);

    const result = await getLabById('ddia-cap-03-lab-01');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/labs/ddia-cap-03-lab-01');
    expect(result).toEqual(mockLab);
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

  it('getInfraStatus deve chamar /api/infra/status', async () => {
    const mockStatus = { postgresReady: true, postgresMessage: 'OK', neo4jReady: true, neo4jMessage: 'OK', timestamp: 1234 };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockStatus
    } as Response);

    const result = await getInfraStatus();

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/infra/status');
    expect(result).toEqual(mockStatus);
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
});
