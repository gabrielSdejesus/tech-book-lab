import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { LabWorkspace } from './LabWorkspace';
import * as api from '../services/api';
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
        starterTemplate: 'CREATE TABLE usuarios (id INT PRIMARY KEY);',
        guidelines: ['Crie chave primária'],
        reflectionPrompt: 'Qual é o impacto do JOIN na latência?'
      }
    ]
  };

  beforeEach(() => {
    vi.clearAllMocks();
    window.confirm = vi.fn(() => true);
  });

  it('deve renderizar os detalhes do laboratório e o template inicial', () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    expect(screen.getByText(/Relacional vs Documentos e Localidade/i)).toBeInTheDocument();
    expect(screen.getByText(/Modelo Relacional Normalizado/i)).toBeInTheDocument();
    expect(screen.getByText(/MOTOR: POSTGRESQL 16/i)).toBeInTheDocument();

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    expect(codeTextarea).toHaveValue('CREATE TABLE usuarios (id INT PRIMARY KEY);');
  });

  it('deve executar a consulta e exibir o resultado tabular na tela', async () => {
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
    fireEvent.click(executeBtn);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
      expect(screen.getByText('15ms')).toBeInTheDocument();
    });
  });

  it('deve solicitar avaliação da IA e exibir parecer do tutor', async () => {
    vi.mocked(api.assessWithAi).mockResolvedValueOnce({
      status: 'APPROVED',
      feedback: 'Sua modelagem relacional segue rigorosamente a 3NF!',
      tradeOffAnalysis: 'Boa separação de entidades versus custo de joins.',
      efficiencyNotes: 'Executou com índice primário.',
      alternativeApproaches: ['Tente adicionar restrição foreign key'],
      modelUsed: 'gemini-3.8-flash'
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    // Digita uma alteração no código para não cair na regra de template inalterado
    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'CREATE TABLE usuarios (id INT PRIMARY KEY, nome VARCHAR(100));' } });

    // Digita reflexão
    const reflectionInput = screen.getByPlaceholderText(/Digite aqui sua análise sobre os trade-offs/i);
    fireEvent.change(reflectionInput, { target: { value: 'Os múltiplos joins aumentam latência de leitura.' } });

    const assessBtn = screen.getByRole('button', { name: /Submeter ao Tutor IA/i });
    fireEvent.click(assessBtn);

    await waitFor(() => {
      expect(screen.getByText(/PARECER: SOLUÇÃO APROVADA/i)).toBeInTheDocument();
      expect(screen.getByText(/Sua modelagem relacional segue rigorosamente a 3NF!/i)).toBeInTheDocument();
      expect(screen.getByText(/Boa separação de entidades versus custo de joins./i)).toBeInTheDocument();
    });
  });

  it('deve chamar resetLab ao clicar em Restaurar', async () => {
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
    fireEvent.click(executeBtn);

    await waitFor(() => {
      expect(screen.getByText(/relation "tabela_fantasma" does not exist/i)).toBeInTheDocument();
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
