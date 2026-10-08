import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getBooks,
  executeQuery,
  resetLab,
  assessWithAi,
  testAiConnection,
  getAiProviders,
  getInfraStatus,
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

  it('getInfraStatus deve realizar GET em /api/infra/status e retornar mapa dinamico de motores', async () => {
    const mockStatus = {
      postgres: { healthy: true, port: 5432, serviceName: 'postgres', status: 'UP' },
      neo4j: { healthy: true, port: 7687, serviceName: 'neo4j', status: 'UP' }
    };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockStatus
    } as Response);

    const result = await getInfraStatus();

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/infra/status', expect.objectContaining({
      headers: expect.objectContaining({ 'X-Session-Id': expect.any(String) })
    }));
    expect(result).toEqual(mockStatus);
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
      body: JSON.stringify({ code: 'SELECT 1;' })
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

    it('executeQuery deve propagar exceção quando fetch falhar por erro de rede', async () => {
      vi.spyOn(globalThis, 'fetch').mockRejectedValueOnce(new TypeError('Network timeout'));

      await expect(executeQuery('SELECT 1;', 'POSTGRES', 'ddia-cap-03-lab-01')).rejects.toThrow('Network timeout');
    });

    it('executeQuery deve usar mensagem de fallback padrão quando HTTP 500 não contiver RFC 7807', async () => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);

      await expect(executeQuery('SELECT 1;', 'POSTGRES', 'ddia-cap-03-lab-01')).rejects.toThrow('Falha na comunicação com o servidor de execução');
    });

    it('resetLab deve usar mensagem de fallback padrão quando HTTP 500 ocorrer', async () => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);

      await expect(resetLab('ddia-cap-03-lab-01')).rejects.toThrow('Falha ao resetar banco do laboratório');
    });

    it('getInfraStatus deve usar mensagem de fallback padrão quando HTTP 500 ocorrer', async () => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);

      await expect(getInfraStatus()).rejects.toThrow('Falha ao consultar status da infraestrutura');
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

    it('executeQuery deve extrair detalhe de sessão expirada (410 GONE) via RFC 7807', async () => {
      const problemDetail = {
        type: 'https://api.dataintensive.lab/errors/session-expired',
        title: 'Sessão Expirada',
        status: 410,
        detail: 'Sessão 1234 expirada há mais de 15 minutos.'
      };

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 410,
        headers: { get: () => 'application/problem+json' },
        json: async () => problemDetail
      } as unknown as Response);

      await expect(executeQuery('SELECT 1;', 'POSTGRES', 'ddia-cap-03-lab-01'))
        .rejects.toThrow('Sessão 1234 expirada há mais de 15 minutos.');
    });
  });
});


