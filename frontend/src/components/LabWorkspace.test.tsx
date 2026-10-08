import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { LabWorkspace } from './LabWorkspace';
import * as api from '../services/api';
import * as provisioningApi from '../services/labProvisioning';
import type { Lab } from '../types';

vi.mock('../services/api', () => ({
  executeQuery: vi.fn(),
  resetLab: vi.fn(),
  assessWithAi: vi.fn(),
  getBooks: vi.fn(),
  testAiConnection: vi.fn(),
  getInfraStatus: vi.fn().mockResolvedValue({}),
  saveChallengeSolution: vi.fn().mockResolvedValue(undefined),
  resetChallengeSolution: vi.fn().mockResolvedValue(undefined),
}));

vi.mock('../services/labProvisioning', () => ({
  provisionLab: vi.fn(),
  getLabStatus: vi.fn(),
  sendHeartbeat: vi.fn(),
  teardownLab: vi.fn().mockResolvedValue({ labId: 'ddia-cap-03-lab-01', status: 'STOPPED', message: 'Ok' }),
}));

describe('LabWorkspace Component', () => {
  const mockLab: Lab = {
    id: 'ddia-cap-03-lab-01',
    number: 1,
    slug: 'relacional-vs-documentos',
    title: 'Relacional vs Documentos e Localidade',
    summary: 'Comparativo de modelos',
    keyConcepts: ['Impedance Mismatch', 'JSONB'],
    engineType: 'POSTGRES',
    databaseName: 'tbl_lab',
    resetSchemaSql: 'DROP TABLE IF EXISTS test;',
    challenges: [
      {
        id: 'lab-01-ch-1',
        order: 1,
        title: 'Modelo Relacional Normalizado (3NF)',
        description: 'Construa as tabelas normalizadas.',
        scenario: 'Crie usuarios e contatos.',
        starterTemplate: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY);',
        guidelines: ['Crie chave primária'],
        reflectionPrompt: 'Qual é o impacto do JOIN na latência?',
        engineType: 'POSTGRES',
      }
    ]
  };

  const mockHybridLab: Lab = {
    id: 'ddia-cap-03-lab-02',
    number: 2,
    slug: 'grafos-vs-relacional',
    title: 'Grafos de Propriedades vs SQL Recursivo',
    summary: 'Comparativo de Grafos e SQL',
    keyConcepts: ['Property Graphs', 'Recursive CTE'],
    engineType: 'NEO4J',
    databaseName: 'tbl_lab_graphs',
    resetSchemaSql: 'MATCH (n) DETACH DELETE n;',
    challenges: [
      {
        id: 'lab-02-ch-1',
        order: 1,
        title: 'Modelagem em Grafo com Cypher',
        description: 'Consulta de rede de relacionamentos.',
        scenario: 'Modelagem no Neo4j.',
        starterTemplate: 'MATCH (p:Person) RETURN p LIMIT 10;',
        guidelines: [],
        reflectionPrompt: 'Vantagens do Cypher?',
        engineType: 'NEO4J',
      },
      {
        id: 'lab-02-ch-2',
        order: 2,
        title: 'Equivalência com SQL Recursivo (Postgres)',
        description: 'Consulta hierárquica usando CTE recursiva.',
        scenario: 'Modelagem relacional hierárquica.',
        starterTemplate: 'WITH RECURSIVE subordinates AS (...) SELECT * FROM subordinates;',
        guidelines: [],
        reflectionPrompt: 'Trade-offs de CTE recursiva?',
        engineType: 'POSTGRES',
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    window.confirm = vi.fn(() => true);

    vi.mocked(provisioningApi.provisionLab).mockResolvedValue({
      labId: 'ddia-cap-03-lab-01',
      engineType: 'POSTGRES',
      status: 'READY',
      message: 'Ambiente pronto',
      allocatedPort: 5432,
      estimatedWaitSeconds: 0,
    });
    vi.mocked(provisioningApi.getLabStatus).mockResolvedValue({
      labId: 'ddia-cap-03-lab-01',
      engineType: 'POSTGRES',
      status: 'READY',
      allocatedPort: 5432,
      uptimeSeconds: 10,
      lastHeartbeatAt: Date.now(),
    });
    vi.mocked(provisioningApi.sendHeartbeat).mockResolvedValue({
      status: 'ALIVE',
      labId: 'ddia-cap-03-lab-01',
      ttlRemainingSeconds: 600,
      lastHeartbeatAt: Date.now(),
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('deve renderizar os detalhes do laboratório e o template inicial', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    expect(screen.getByText(/Relacional vs Documentos e Localidade/i)).toBeInTheDocument();
    expect(screen.getByText(/Modelo Relacional Normalizado/i)).toBeInTheDocument();
    expect(screen.getByText(/MOTOR: POSTGRESQL 16/i)).toBeInTheDocument();

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    expect(codeTextarea).toHaveValue('CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY);');

    await waitFor(() => expect(api.getInfraStatus).toHaveBeenCalled());
  });

  it('deve executar a consulta e exibir o resultado tabular na tela quando READY', async () => {
    vi.mocked(api.executeQuery).mockResolvedValueOnce({
      success: true,
      message: 'Consulta executada com sucesso.',
      columns: ['id', 'nome'],
      rows: [{ id: 1, nome: 'Alice' }],
      rowCount: 1,
      executionTimeMs: 15,
      errorMessage: null
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const executeBtn = screen.getByRole('button', { name: /Executar/i });
    await waitFor(() => expect(executeBtn).not.toBeDisabled());
    fireEvent.click(executeBtn);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
      expect(screen.getByText('15ms')).toBeInTheDocument();
    });
  });

  it('deve renderizar a aba "Validação Offline" e os botões "Validar Solução (Offline)" e "Consultar Tutor IA"', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    expect(screen.getByRole('button', { name: /Validar Solução \(Offline\)/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Consultar Tutor IA/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Validação Offline/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Tutor IA$/i })).toBeInTheDocument();
  });

  it('deve executar a validação offline ao clicar em "Validar Solução (Offline)" e exibir o parecer determinístico', async () => {
    vi.mocked(api.assessWithAi).mockResolvedValueOnce({
      status: 'APPROVED',
      feedback: 'Modelagem 3NF validada com sucesso pelo motor heurístico!',
      tradeOffAnalysis: 'Excelente separação de entidades relacionais.',
      efficiencyNotes: 'Execução local determinística.',
      alternativeApproaches: ['Adicione índices em chaves estrangeiras'],
      modelUsed: 'Tutor Heurístico (Regras Locais)'
    });

    render(<LabWorkspace lab={mockLab} apiKey="" provider="heuristic" model="rules-engine-v1" />);

    const validateBtn = screen.getByRole('button', { name: /Validar Solução \(Offline\)/i });
    await waitFor(() => expect(validateBtn).not.toBeDisabled());

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));' } });

    fireEvent.click(validateBtn);

    await waitFor(() => {
      expect(api.assessWithAi).toHaveBeenCalledWith(
        expect.objectContaining({
          providerOverride: 'heuristic'
        })
      );
      expect(screen.getByText(/Modelagem 3NF validada com sucesso pelo motor heurístico!/i)).toBeInTheDocument();
      expect(screen.getByText(/Motor Heurístico DDIA \(Regras Locais\) • Modo Offline/i)).toBeInTheDocument();
    });
  });

  it('deve solicitar avaliação da IA e exibir parecer com os 4 subtópicos ao clicar em "Consultar Tutor IA" com sucesso', async () => {
    vi.mocked(api.assessWithAi).mockResolvedValueOnce({
      status: 'APPROVED',
      feedback: 'Sua modelagem relacional segue rigorosamente a 3NF!',
      tradeOffAnalysis: 'Boa separação de entidades versus custo de joins.',
      efficiencyNotes: 'Executou com índice primário.',
      alternativeApproaches: ['Tente adicionar restrição foreign key'],
      modelUsed: 'gemini-3.8-flash'
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const aiBtn = screen.getByRole('button', { name: /Consultar Tutor IA/i });
    await waitFor(() => expect(aiBtn).not.toBeDisabled());

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));' } });

    const reflectionInput = screen.getByPlaceholderText(/Digite aqui sua análise sobre os trade-offs/i);
    fireEvent.change(reflectionInput, { target: { value: 'Os múltiplos joins aumentam latência de leitura.' } });

    fireEvent.click(aiBtn);

    await waitFor(() => {
      expect(screen.getByText(/PARECER: SOLUÇÃO APROVADA/i)).toBeInTheDocument();
      expect(screen.getByText(/Análise Crítica do Tutor/i)).toBeInTheDocument();
      expect(screen.getByText(/Trade-offs Teóricos \(Martin Kleppmann - DDIA\)/i)).toBeInTheDocument();
      expect(screen.getByText(/Observações de Desempenho & Custo Computacional/i)).toBeInTheDocument();
      expect(screen.getByText(/Abordagens Alternativas Válidas/i)).toBeInTheDocument();
    });
  });

  it('deve exibir banner simplificado de autenticação pendente sem os 4 subtópicos pedagógicos ao consultar Tutor IA sem chave', async () => {
    vi.mocked(api.assessWithAi).mockResolvedValueOnce({
      status: 'NEEDS_REVISION',
      feedback: 'Chave de API não informada para o Google Gemini. Insira sua chave no modal de configurações.',
      tradeOffAnalysis: '',
      efficiencyNotes: '',
      alternativeApproaches: [],
      modelUsed: 'Pending Authentication'
    });

    render(<LabWorkspace lab={mockLab} apiKey="" provider="gemini" model="gemini-3.8-flash" />);

    const aiBtn = screen.getByRole('button', { name: /Consultar Tutor IA/i });
    await waitFor(() => expect(aiBtn).not.toBeDisabled());

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));' } });

    fireEvent.click(aiBtn);

    await waitFor(() => {
      expect(api.assessWithAi).toHaveBeenCalledWith(
        expect.objectContaining({
          providerOverride: 'gemini',
          apiKeyOverride: ''
        })
      );
      expect(screen.getByText(/Chave de API Não Configurada/i)).toBeInTheDocument();
      expect(screen.getByText(/Chave de API não informada para o Google Gemini/i)).toBeInTheDocument();
      // Não deve renderizar os 4 subtópicos
      expect(screen.queryByText(/Análise Crítica do Tutor/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Trade-offs Teóricos \(Martin Kleppmann - DDIA\)/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Observações de Desempenho & Custo Computacional/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Abordagens Alternativas Válidas/i)).not.toBeInTheDocument();
    });
  });

  it('deve exibir banner simplificado de erro sem os 4 subtópicos pedagógicos ao ocorrer falha de comunicação com a IA', async () => {
    vi.mocked(api.assessWithAi).mockResolvedValueOnce({
      status: 'NEEDS_REVISION',
      feedback: 'Falha na comunicação com o Tutor de IA (gemini): Connection timeout.',
      tradeOffAnalysis: '',
      efficiencyNotes: '',
      alternativeApproaches: [],
      modelUsed: 'AI Error'
    });

    render(<LabWorkspace lab={mockLab} apiKey="valid-key" provider="gemini" model="gemini-3.8-flash" />);

    const aiBtn = screen.getByRole('button', { name: /Consultar Tutor IA/i });
    await waitFor(() => expect(aiBtn).not.toBeDisabled());

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));' } });

    fireEvent.click(aiBtn);

    await waitFor(() => {
      expect(screen.getByText(/Falha na Comunicação com o Tutor IA/i)).toBeInTheDocument();
      expect(screen.getByText(/Connection timeout/i)).toBeInTheDocument();
      // Não deve renderizar os 4 subtópicos
      expect(screen.queryByText(/Análise Crítica do Tutor/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Trade-offs Teóricos \(Martin Kleppmann - DDIA\)/i)).not.toBeInTheDocument();
    });
  });

  it('deve chamar resetLab ao clicar em Restaurar quando READY', async () => {
    vi.mocked(api.resetLab).mockResolvedValueOnce({
      success: true,
      message: 'Banco limpo restaurado com sucesso.',
      columns: [],
      rows: [],
      rowCount: 0,
      executionTimeMs: 5,
      errorMessage: null
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const resetBtn = screen.getByRole('button', { name: /Restaurar/i });
    await waitFor(() => expect(resetBtn).not.toBeDisabled());
    fireEvent.click(resetBtn);

    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalled();
      expect(api.resetLab).toHaveBeenCalledWith('ddia-cap-03-lab-01');
      expect(screen.getByText(/Esquema restaurado para o estado original com sucesso!/i)).toBeInTheDocument();
    });
  });

  it('deve exibir mensagem de erro amigável ao falhar execução com erro 400 (RFC 7807)', async () => {
    vi.mocked(api.executeQuery).mockRejectedValueOnce(
      new Error('Erro na execução da consulta: relation "tabela_fantasma" does not exist')
    );

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const executeBtn = screen.getByRole('button', { name: /Executar/i });
    await waitFor(() => expect(executeBtn).not.toBeDisabled());
    fireEvent.click(executeBtn);

    await waitFor(() => {
      expect(screen.getByText(/relation "tabela_fantasma" does not exist/i)).toBeInTheDocument();
    });
  });

  it('deve exibir o painel de erro com título e mensagem detalhada quando a execução retornar success false', async () => {
    vi.mocked(api.executeQuery).mockResolvedValueOnce({
      success: false,
      message: 'Falha na execução',
      columns: [],
      rows: [],
      rowCount: 0,
      executionTimeMs: 15,
      errorMessage: 'PSQLException: ERROR: syntax error at or near "SELCT"'
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const executeBtn = screen.getByRole('button', { name: /Executar/i });
    await waitFor(() => expect(executeBtn).not.toBeDisabled());
    fireEvent.click(executeBtn);

    await waitFor(() => {
      expect(screen.getByText('Erro de Execução no Banco')).toBeInTheDocument();
      expect(screen.getByText(/PSQLException: ERROR: syntax error at or near "SELCT"/i)).toBeInTheDocument();
    });
  });

  it('deve capturar falha na execução disparada pelo atalho Ctrl + Enter no editor de código', async () => {
    vi.mocked(api.executeQuery).mockRejectedValueOnce(
      new Error('Timeout durante execução de consulta pesada')
    );

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const textarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    await waitFor(() => expect(screen.getByRole('button', { name: /Executar/i })).not.toBeDisabled());

    fireEvent.keyDown(textarea, { ctrlKey: true, key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText(/Timeout durante execução de consulta pesada/i)).toBeInTheDocument();
    });
  });

  it('deve bloquear defensivamente o botão de execução e exibir banner enquanto status for PROVISIONING', async () => {
    vi.mocked(provisioningApi.provisionLab).mockReturnValueOnce(
      new Promise(() => {}) // never resolves to keep PROVISIONING state
    );

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const executeBtn = screen.getByRole('button', { name: /Executar/i });
    expect(executeBtn).toBeDisabled();
    expect(executeBtn).toHaveAttribute('title', 'Aguarde o provisionamento do banco de dados...');

    expect(screen.getByText(/Provisionando ambiente de laboratório para a tarefa.../i)).toBeInTheDocument();

    const badge = screen.getByTestId('active-engine-badge');
    expect(badge).toHaveTextContent('PG:5432');
    expect(badge).toHaveTextContent('BOOTING');

    await waitFor(() => expect(api.getInfraStatus).toHaveBeenCalled());
  });

  it('deve exibir o indicador contextual de motor com status ON quando READY', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    await waitFor(() => {
      const badge = screen.getByTestId('active-engine-badge');
      expect(badge).toHaveTextContent('PG:5432');
      expect(badge).toHaveTextContent('ON');
    });
  });

  it('deve provisionar o motor específico do desafio ao alternar desafios em laboratório híbrido', async () => {
    vi.mocked(provisioningApi.provisionLab).mockResolvedValue({
      labId: 'ddia-cap-03-lab-02',
      challengeId: 'lab-02-ch-1',
      engineType: 'NEO4J',
      status: 'READY',
      message: 'Neo4j pronto',
      allocatedPort: 7687,
      estimatedWaitSeconds: 0,
    });

    render(<LabWorkspace lab={mockHybridLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    // Initial challenge 1 (NEO4J)
    await waitFor(() => {
      expect(provisioningApi.provisionLab).toHaveBeenCalledWith('ddia-cap-03-lab-02', 'lab-02-ch-1');
      const badge = screen.getByTestId('active-engine-badge');
      expect(badge).toHaveTextContent('NEO4J:7687');
      expect(screen.getByText(/MOTOR: NEO4J 5/i)).toBeInTheDocument();
    });

    // Switch to challenge 2 (POSTGRES)
    const exercise2Tab = screen.getByRole('button', { name: /Exercício 2/i });
    fireEvent.click(exercise2Tab);

    await waitFor(() => {
      expect(provisioningApi.provisionLab).toHaveBeenCalledWith('ddia-cap-03-lab-02', 'lab-02-ch-2');
      const badge = screen.getByTestId('active-engine-badge');
      expect(badge).toHaveTextContent('PG:5432');
      expect(screen.getByText(/MOTOR: POSTGRESQL 16/i)).toBeInTheDocument();
    });
  });

  it('deve possuir affordance de cursor-pointer e disabled:cursor-not-allowed nos botões de ação', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const executeBtn = screen.getByRole('button', { name: /Executar/i });
    expect(executeBtn).toHaveClass('disabled:cursor-not-allowed');

    const validateBtn = screen.getByRole('button', { name: /Validar Solução \(Offline\)/i });
    expect(validateBtn).toHaveClass('disabled:cursor-not-allowed');

    const assessBtn = screen.getByRole('button', { name: /Consultar Tutor IA/i });
    expect(assessBtn).toHaveClass('disabled:cursor-not-allowed');

    const resetBtn = screen.getByRole('button', { name: /Restaurar/i });
    expect(resetBtn).toHaveClass('disabled:cursor-not-allowed');

    await waitFor(() => expect(api.getInfraStatus).toHaveBeenCalled());
  });

  it('deve carregar e renderizar porta e status dinâmico a partir de getInfraStatus', async () => {
    vi.mocked(api.getInfraStatus).mockResolvedValueOnce({
      postgres: { healthy: true, port: 5433, serviceName: 'postgres', status: 'UP' }
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    await waitFor(() => {
      expect(api.getInfraStatus).toHaveBeenCalled();
      const badge = screen.getByTestId('active-engine-badge');
      expect(badge).toHaveTextContent('PG:5433');
    });
  });

  it('deve exibir rótulo de conexão e estilização dinamicamente a partir do engineConfig para motores customizados', async () => {
    const customLab: Lab = {
      ...mockLab,
      id: 'custom-lab-redis',
      engineType: 'REDIS' as any,
      challenges: [
        {
          ...mockLab.challenges[0],
          id: 'redis-ch-1',
          engineType: 'REDIS' as any,
        },
      ],
    };

    vi.mocked(provisioningApi.provisionLab).mockResolvedValue({
      labId: 'custom-lab-redis',
      challengeId: 'redis-ch-1',
      engineType: 'REDIS',
      status: 'READY',
      message: 'Redis pronto',
      allocatedPort: 6379,
      estimatedWaitSeconds: 0,
    });

    render(<LabWorkspace lab={customLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    await waitFor(() => {
      const badge = screen.getByTestId('active-engine-badge');
      expect(badge).toHaveTextContent('REDIS');
      expect(badge).not.toHaveTextContent('PG:5432');
    });
  });

  it('deve chamar teardownLab ao desmontar o componente LabWorkspace', async () => {
    const { unmount } = render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    await waitFor(() => {
      expect(provisioningApi.provisionLab).toHaveBeenCalledWith(mockLab.id, mockLab.challenges[0].id);
    });

    unmount();

    expect(provisioningApi.teardownLab).toHaveBeenCalledWith(mockLab.id);
  });

  it('deve agendar heartbeat respeitando o heartbeatIntervalSeconds retornado pelo provisionamento', async () => {
    vi.useFakeTimers();

    vi.mocked(provisioningApi.provisionLab).mockResolvedValueOnce({
      labId: 'ddia-cap-03-lab-01',
      engineType: 'POSTGRES',
      status: 'READY',
      message: 'Ambiente pronto',
      allocatedPort: 5432,
      estimatedWaitSeconds: 0,
      heartbeatIntervalSeconds: 10,
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    await vi.waitFor(() => {
      expect(provisioningApi.provisionLab).toHaveBeenCalled();
    });

    expect(provisioningApi.sendHeartbeat).not.toHaveBeenCalled();

    vi.advanceTimersByTime(10000);
    expect(provisioningApi.sendHeartbeat).toHaveBeenCalledWith('ddia-cap-03-lab-01');

    vi.useRealTimers();
  });

  it('deve bloquear envio ao Tutor IA via validação local se a query for apenas comentários ou template inalterado', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const assessBtn = screen.getByRole('button', { name: /Consultar Tutor IA/i });
    await waitFor(() => expect(assessBtn).not.toBeDisabled());

    // Clica sem alterar o template
    fireEvent.click(assessBtn);

    await waitFor(() => {
      expect(api.assessWithAi).not.toHaveBeenCalled();
      expect(screen.getByText(/Nenhuma implementação submetida/i)).toBeInTheDocument();
      expect(screen.getByText(/Validador Local de Submissão/i)).toBeInTheDocument();
    });

    // Agora digita apenas comentários
    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: '-- Apenas um comentário\n/* outro comentário */' } });

    fireEvent.click(assessBtn);

    await waitFor(() => {
      expect(api.assessWithAi).not.toHaveBeenCalled();
      expect(screen.getByText(/Nenhuma implementação submetida/i)).toBeInTheDocument();
    });
  });

  it('não deve chamar resetLab quando o usuário cancelar a confirmação de restauração', async () => {
    vi.mocked(window.confirm).mockReturnValueOnce(false);

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const resetBtn = screen.getByRole('button', { name: /Restaurar/i });
    await waitFor(() => expect(resetBtn).not.toBeDisabled());
    fireEvent.click(resetBtn);

    expect(window.confirm).toHaveBeenCalled();
    expect(api.resetLab).not.toHaveBeenCalled();
  });

  it('deve disparar execução da consulta ao pressionar Ctrl + Enter no textarea', async () => {
    vi.mocked(api.executeQuery).mockResolvedValueOnce({
      success: true,
      message: 'Consulta executada',
      columns: ['num'],
      rows: [{ num: 42 }],
      rowCount: 1,
      executionTimeMs: 8,
      errorMessage: null,
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    await waitFor(() => expect(screen.getByRole('button', { name: /Executar/i })).not.toBeDisabled());

    fireEvent.keyDown(codeTextarea, { key: 'Enter', ctrlKey: true });

    await waitFor(() => {
      expect(api.executeQuery).toHaveBeenCalledWith(
        mockLab.challenges[0].starterTemplate,
        'POSTGRES',
        mockLab.id
      );
      expect(screen.getByText('42')).toBeInTheDocument();
    });
  });

  it('deve permitir visualizar resultado no formato JSON e restaurar template inicial via botão Recarregar Template', async () => {
    vi.mocked(api.executeQuery).mockResolvedValueOnce({
      success: true,
      message: 'Ok',
      columns: ['id'],
      rows: [{ id: 99 }],
      rowCount: 1,
      executionTimeMs: 10,
      errorMessage: null,
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const executeBtn = screen.getByRole('button', { name: /Executar/i });
    await waitFor(() => expect(executeBtn).not.toBeDisabled());
    fireEvent.click(executeBtn);

    await waitFor(() => {
      expect(screen.getByText('99')).toBeInTheDocument();
    });

    // Clica na aba JSON
    const jsonTabBtn = screen.getByRole('button', { name: /JSON/i });
    fireEvent.click(jsonTabBtn);

    await waitFor(() => {
      expect(screen.getByText(/"rowCount": 1/i)).toBeInTheDocument();
      expect(screen.getByText(/"id": 99/i)).toBeInTheDocument();
    });

    // Modifica código no textarea
    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'SELECT * FROM custom;' } });
    expect(codeTextarea).toHaveValue('SELECT * FROM custom;');

    // Clica em Recarregar Template
    const reloadBtn = screen.getByRole('button', { name: /Recarregar Template/i });
    fireEvent.click(reloadBtn);
    expect(codeTextarea).toHaveValue(mockLab.challenges[0].starterTemplate);
  });

  it('deve realizar polling de getLabStatus quando provisionLab retornar PROVISIONING e atualizar para READY', async () => {
    vi.useFakeTimers();

    vi.mocked(provisioningApi.provisionLab).mockResolvedValueOnce({
      labId: 'ddia-cap-03-lab-01',
      engineType: 'POSTGRES',
      status: 'PROVISIONING',
      message: 'Criando container...',
      allocatedPort: 5432,
      estimatedWaitSeconds: 5,
    });

    vi.mocked(provisioningApi.getLabStatus).mockResolvedValueOnce({
      labId: 'ddia-cap-03-lab-01',
      engineType: 'POSTGRES',
      status: 'READY',
      allocatedPort: 5432,
      uptimeSeconds: 2,
      lastHeartbeatAt: Date.now(),
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    await vi.waitFor(() => {
      expect(provisioningApi.provisionLab).toHaveBeenCalled();
    });

    await vi.waitFor(() => {
      expect(screen.getByText(/Criando container.../i)).toBeInTheDocument();
    });

    await vi.advanceTimersByTimeAsync(2000);

    await vi.waitFor(() => {
      expect(provisioningApi.getLabStatus).toHaveBeenCalledWith('ddia-cap-03-lab-01', mockLab.challenges[0].id);
      const badge = screen.getByTestId('active-engine-badge');
      expect(badge).toHaveTextContent('ON');
    });

    vi.useRealTimers();
  });

  it('deve exibir banner de erro com botão Tentar Novamente quando o status for ERROR', async () => {
    vi.mocked(provisioningApi.provisionLab).mockResolvedValueOnce({
      labId: 'ddia-cap-03-lab-01',
      engineType: 'POSTGRES',
      status: 'ERROR',
      message: 'Container falhou ao iniciar',
      allocatedPort: 5432,
      estimatedWaitSeconds: 0,
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    await waitFor(() => {
      expect(screen.getByText(/Container falhou ao iniciar/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Tentar Novamente/i })).toBeInTheDocument();
    });

    // Mock do retry
    vi.mocked(provisioningApi.provisionLab).mockResolvedValueOnce({
      labId: 'ddia-cap-03-lab-01',
      engineType: 'POSTGRES',
      status: 'READY',
      message: 'Ambiente recuperado',
      allocatedPort: 5432,
      estimatedWaitSeconds: 0,
    });

    const retryBtn = screen.getByRole('button', { name: /Tentar Novamente/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      const badge = screen.getByTestId('active-engine-badge');
      expect(badge).toHaveTextContent('ON');
    });
  });

  it('deve carregar o starter template no editor contendo apenas código executável sem comentários', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i) as HTMLTextAreaElement;
    expect(codeTextarea.value).not.toMatch(/--|\/\/|\/\*|\*\//);
    expect(codeTextarea.value).toContain('CREATE TABLE IF NOT EXISTS usuarios');
  });

  it('deve priorizar savedCode em relação a starterTemplate ao carregar o laboratório', async () => {
    const labWithSavedCode: Lab = {
      ...mockLab,
      challenges: [
        {
          ...mockLab.challenges[0],
          savedCode: 'SELECT id, nome FROM usuarios WHERE id = 42;'
        }
      ]
    };

    render(<LabWorkspace lab={labWithSavedCode} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i) as HTMLTextAreaElement;
    expect(codeTextarea.value).toBe('SELECT id, nome FROM usuarios WHERE id = 42;');
  });

  it('deve acionar auto-save com debounce de 600ms após digitação no editor', async () => {
    vi.useFakeTimers();

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'SELECT * FROM usuarios WHERE ativo = true;' } });

    // Após 300ms, ainda não deve ter chamado
    vi.advanceTimersByTime(300);
    expect(api.saveChallengeSolution).not.toHaveBeenCalled();

    // Após mais 350ms (total > 600ms), deve ter chamado
    vi.advanceTimersByTime(350);
    expect(api.saveChallengeSolution).toHaveBeenCalledWith('lab-01-ch-1', 'SELECT * FROM usuarios WHERE ativo = true;');

    vi.useRealTimers();
  });

  it('deve preservar o código digitado ao alternar entre exercícios do laboratório', async () => {
    render(<LabWorkspace lab={mockHybridLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    // Digita no Exercício 1
    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'MATCH (u:User) RETURN u;' } });

    // Alterna para o Exercício 2
    const exercise2Tab = screen.getByRole('button', { name: /Exercício 2/i });
    fireEvent.click(exercise2Tab);

    expect(codeTextarea).toHaveValue(mockHybridLab.challenges[1].starterTemplate);

    // Retorna ao Exercício 1
    const exercise1Tab = screen.getByRole('button', { name: /Exercício 1/i });
    fireEvent.click(exercise1Tab);

    // Deve preservar o que foi digitado
    expect(codeTextarea).toHaveValue('MATCH (u:User) RETURN u;');
  });

  it('deve chamar resetChallengeSolution e restaurar starterTemplate ao clicar em Recarregar Template', async () => {
    const labWithSavedCode: Lab = {
      ...mockLab,
      challenges: [
        {
          ...mockLab.challenges[0],
          savedCode: 'SELECT custom_code;'
        }
      ]
    };

    render(<LabWorkspace lab={labWithSavedCode} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    expect(codeTextarea).toHaveValue('SELECT custom_code;');

    const reloadBtn = screen.getByRole('button', { name: /Recarregar Template/i });
    fireEvent.click(reloadBtn);

    await waitFor(() => {
      expect(api.resetChallengeSolution).toHaveBeenCalledWith('lab-01-ch-1');
      expect(codeTextarea).toHaveValue(mockLab.challenges[0].starterTemplate);
    });
  });

  it('deve renderizar colunas e linhas do último SELECT após executar script SQL com múltiplos comandos', async () => {
    vi.mocked(api.executeQuery).mockResolvedValueOnce({
      success: true,
      message: 'Consulta executada com sucesso.',
      columns: ['id', 'titulo', 'ano'],
      rows: [
        { id: 1, titulo: 'DDIA', ano: 2017 },
        { id: 2, titulo: 'Designing Data-Intensive Apps', ano: 2026 }
      ],
      rowCount: 2,
      executionTimeMs: 25,
      errorMessage: null
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, {
      target: {
        value: `
          CREATE TABLE livros (id INT, titulo VARCHAR(100), ano INT);
          INSERT INTO livros VALUES (1, 'DDIA', 2017), (2, 'Designing Data-Intensive Apps', 2026);
          SELECT id, titulo, ano FROM livros ORDER BY id ASC;
        `
      }
    });

    const executeBtn = screen.getByRole('button', { name: /Executar/i });
    await waitFor(() => expect(executeBtn).not.toBeDisabled());
    fireEvent.click(executeBtn);

    await waitFor(() => {
      expect(screen.getByText('DDIA')).toBeInTheDocument();
      expect(screen.getByText('Designing Data-Intensive Apps')).toBeInTheDocument();
      expect(screen.getByText('25ms')).toBeInTheDocument();
    });
  });
});


