import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getBooks,
  assessWithAi,
  testAiConnection,
  getAiProviders,
  saveChallengeSolution,
  resetChallengeSolution,
  getLabScenarios,
  saveScenarioSolution,
  getChapterQuiz,
  submitChapterQuiz,
  stressTestScenario,
  assessScenario
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

  it('getLabScenarios deve realizar GET em /api/labs/:id/scenarios e retornar cenarios interativos', async () => {
    const mockScenarios = [
      { id: 'sc-1', labId: 'lab-1', title: 'Cenário 1', scenarioType: 'CANVAS_MODELING' }
    ];
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockScenarios
    } as Response);

    const result = await getLabScenarios('lab-1', 'en');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/labs/lab-1/scenarios', expect.objectContaining({
      headers: expect.objectContaining({
        'Accept-Language': 'en'
      })
    }));
    expect(result).toEqual(mockScenarios);
  });

  it('saveScenarioSolution deve realizar PUT em /api/scenarios/:id/solution com body JSON', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({})
    } as Response);

    const payload = { userStateJson: '{"nodes":[]}', userReflection: 'Arquitetura validada' };
    await saveScenarioSolution('sc-1', payload);

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/scenarios/sc-1/solution', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({
        'Content-Type': 'application/json'
      }),
      body: JSON.stringify(payload)
    }));
  });

  it('getChapterQuiz deve realizar GET em /api/chapters/:id/quiz e retornar simulado', async () => {
    const mockQuiz = { chapterId: 'cap-3', questions: [], totalQuestions: 6, totalAnswered: 0, totalCorrect: 0 };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockQuiz
    } as Response);

    const result = await getChapterQuiz('cap-3', 'pt');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/chapters/cap-3/quiz', expect.objectContaining({
      headers: expect.objectContaining({
        'Accept-Language': 'pt'
      })
    }));
    expect(result).toEqual(mockQuiz);
  });

  it('getChapterQuiz com bookId deve incluir bookId no caminho da rota', async () => {
    const mockQuiz = { chapterId: 'cap-3', questions: [], totalQuestions: 6, totalAnswered: 0, totalCorrect: 0 };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockQuiz
    } as Response);

    await getChapterQuiz('cap-3', 'en', 'ddia');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/books/ddia/chapters/cap-3/quiz', expect.objectContaining({
      headers: expect.objectContaining({
        'Accept-Language': 'en'
      })
    }));
  });

  it('submitChapterQuiz deve realizar POST em /api/chapters/:id/quiz/submit e retornar resultado', async () => {
    const mockResponse = { chapterId: 'cap-3', totalQuestions: 2, correctAnswers: 2, scorePercentage: 100, results: [] };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse
    } as Response);

    const submission = { answers: { 'q-1': 'opt-1' } };
    const result = await submitChapterQuiz('cap-3', submission);

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/chapters/cap-3/quiz/submit', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json'
      }),
      body: JSON.stringify(submission)
    }));
    expect(result).toEqual(mockResponse);
  });

  it('stressTestScenario deve realizar POST em /api/scenarios/:id/stress-test com body e cabeçalho Accept-Language', async () => {
    const mockStressResponse = {
      totalRequests: 1000,
      successfulRequests: 1000,
      failedRequests: 0,
      actualThroughputRps: 1000,
      p50LatencyMs: 4.5,
      p99LatencyMs: 18.2,
      lockContentionIndex: 0.05,
      slaMet: true,
      diagnostics: ['Arquitetura altamente escalável'],
      nodeBottlenecks: {}
    };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockStressResponse
    } as Response);

    const req = {
      modelJson: '{"nodes":[],"edges":[]}',
      workload: {
        concurrency: 50,
        targetRps: 1000,
        durationSeconds: 10,
        readRatio: 0.8,
        keyDistribution: 'ZIPFIAN' as const,
        slaLatencyMs: 50
      }
    };
    const result = await stressTestScenario('sc-1', req, 'en');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/scenarios/sc-1/stress-test', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'Accept-Language': 'en'
      }),
      body: JSON.stringify(req)
    }));
    expect(result).toEqual(mockStressResponse);
  });

  it('assessScenario deve realizar POST em /api/scenarios/:id/assess com body e cabeçalho Accept-Language', async () => {
    const mockAssessResponse = {
      status: 'APPROVED',
      feedback: 'Arquitetura muito bem projetada.',
      architecturalAnalysis: 'Particionamento eficiente.',
      executionAnalysis: 'Latência baixa'
    };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockAssessResponse
    } as Response);

    const req = {
      architectureJson: '{"nodes":[]}',
      userReflection: 'Optamos por cache Redis para desonerar o PostgreSQL.',
      provider: 'gemini',
      model: 'gemini-2.5-flash'
    };
    const result = await assessScenario('sc-1', req, 'pt');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/scenarios/sc-1/assess', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        'Accept-Language': 'pt'
      }),
      body: JSON.stringify(req)
    }));
    expect(result).toEqual(mockAssessResponse);
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

    it('getLabScenarios, saveScenarioSolution, getChapterQuiz e submitChapterQuiz devem lançar fallbacks amigáveis ao falharem', async () => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);
      await expect(getLabScenarios('lab-1')).rejects.toThrow('Falha ao carregar cenários interativos');

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);
      await expect(saveScenarioSolution('sc-1', { userStateJson: '{}' })).rejects.toThrow('Falha ao salvar solução do cenário interativo');

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);
      await expect(getChapterQuiz('cap-1')).rejects.toThrow('Falha ao carregar simulado do capítulo');

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);
      await expect(submitChapterQuiz('cap-1', { answers: {} })).rejects.toThrow('Falha ao submeter simulado do capítulo');

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);
      await expect(stressTestScenario('sc-1', {
        modelJson: '{}',
        workload: { concurrency: 10, targetRps: 100, durationSeconds: 5, readRatio: 0.5, keyDistribution: 'UNIFORM', slaLatencyMs: 50 }
      })).rejects.toThrow('Falha ao executar teste de estresse da arquitetura');

      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'text/plain' },
        json: async () => ({})
      } as unknown as Response);
      await expect(assessScenario('sc-1', {
        architectureJson: '{}',
        userReflection: 'Reflexao'
      })).rejects.toThrow('Falha ao avaliar arquitetura com Tutor de IA');
    });
  });
});
