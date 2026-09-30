import { describe, it, expect, vi, beforeEach } from 'vitest';
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
  getLabById: vi.fn(),
  getInfraStatus: vi.fn(),
  testAiConnection: vi.fn(),
}));

vi.mock('../services/labProvisioning', () => ({
  provisionLab: vi.fn(),
  getLabStatus: vi.fn(),
  sendHeartbeat: vi.fn(),
  teardownLab: vi.fn(),
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
    vi.mocked(provisioningApi.teardownLab).mockResolvedValue({
      labId: 'ddia-cap-03-lab-01',
      status: 'STOPPED',
      message: 'Container parado',
    });
  });

  it('deve renderizar os detalhes do laboratório e o template inicial', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    expect(screen.getByText(/Relacional vs Documentos e Localidade/i)).toBeInTheDocument();
    expect(screen.getByText(/Modelo Relacional Normalizado/i)).toBeInTheDocument();
    expect(screen.getByText(/MOTOR: POSTGRESQL 16/i)).toBeInTheDocument();

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    expect(codeTextarea).toHaveValue('CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY);');
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

  it('deve solicitar avaliação da IA e exibir parecer do tutor quando READY', async () => {
    vi.mocked(api.assessWithAi).mockResolvedValueOnce({
      status: 'APPROVED',
      feedback: 'Sua modelagem relacional segue rigorosamente a 3NF!',
      tradeOffAnalysis: 'Boa separação de entidades versus custo de joins.',
      efficiencyNotes: 'Executou com índice primário.',
      alternativeApproaches: ['Tente adicionar restrição foreign key'],
      modelUsed: 'gemini-3.8-flash'
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const assessBtn = screen.getByRole('button', { name: /Submeter ao Tutor IA/i });
    await waitFor(() => expect(assessBtn).not.toBeDisabled());

    // Digita uma alteração no código para não cair na regra de template inalterado
    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));' } });

    // Digita reflexão
    const reflectionInput = screen.getByPlaceholderText(/Digite aqui sua análise sobre os trade-offs/i);
    fireEvent.change(reflectionInput, { target: { value: 'Os múltiplos joins aumentam latência de leitura.' } });

    fireEvent.click(assessBtn);

    await waitFor(() => {
      expect(screen.getByText(/PARECER: SOLUÇÃO APROVADA/i)).toBeInTheDocument();
      expect(screen.getByText(/Sua modelagem relacional segue rigorosamente a 3NF!/i)).toBeInTheDocument();
      expect(screen.getByText(/Boa separação de entidades versus custo de joins./i)).toBeInTheDocument();
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

  it('deve possuir affordance de cursor-pointer e disabled:cursor-not-allowed nos botões de ação', () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const executeBtn = screen.getByRole('button', { name: /Executar/i });
    expect(executeBtn).toHaveClass('disabled:cursor-not-allowed');

    const assessBtn = screen.getByRole('button', { name: /Submeter ao Tutor IA/i });
    expect(assessBtn).toHaveClass('disabled:cursor-not-allowed');

    const resetBtn = screen.getByRole('button', { name: /Restaurar/i });
    expect(resetBtn).toHaveClass('disabled:cursor-not-allowed');
  });
});
