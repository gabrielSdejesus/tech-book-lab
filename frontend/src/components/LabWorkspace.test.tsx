import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { LabWorkspace } from './LabWorkspace';
import * as api from '../services/api';
import type { Lab } from '../types';

vi.mock('../services/api', () => ({
  assessWithAi: vi.fn(),
  getBooks: vi.fn(),
  testAiConnection: vi.fn(),
  saveChallengeSolution: vi.fn().mockResolvedValue(undefined),
  resetChallengeSolution: vi.fn().mockResolvedValue(undefined),
}));

describe('LabWorkspace Component', () => {
  const mockLab: Lab = {
    id: 'ddia-cap-03-lab-01',
    number: 1,
    slug: 'relacional-vs-documentos',
    title: 'Relacional vs Documentos e Localidade',
    summary: 'Comparativo de modelos de dados',
    keyConcepts: ['Impedance Mismatch', 'JSONB'],
    challenges: [
      {
        id: 'lab-01-ch-1',
        order: 1,
        title: 'Modelo Relacional Normalizado (3NF)',
        description: 'Construa as tabelas normalizadas.',
        scenario: 'Crie usuarios e contatos com integridade referencial.',
        starterTemplate: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY);',
        guidelines: ['Crie chave primária', 'Defina foreign keys'],
        reflectionPrompt: 'Qual é o impacto do JOIN na latência?',
        expectedReflection: 'Gabarito oficial de trade-off para o modelo 3NF',
      },
      {
        id: 'lab-01-ch-2',
        order: 2,
        title: 'Modelo de Documento Desnormalizado',
        description: 'Construa documento JSONB aninhado.',
        scenario: 'Aninhe contatos dentro do registro do usuário.',
        starterTemplate: '{"usuario_id": 1, "contatos": []}',
        guidelines: ['Evite múltiplos joins'],
        reflectionPrompt: 'Trade-offs de desnormalização no armazenamento?',
        expectedReflection: 'Gabarito oficial de desnormalização JSONB',
      }
    ]
  };

  beforeEach(() => {
    vi.clearAllMocks();
    window.confirm = vi.fn(() => true);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('deve renderizar os detalhes do laboratório e o template inicial do desafio', () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    expect(screen.getByText(/Relacional vs Documentos e Localidade/i)).toBeInTheDocument();
    expect(screen.getByText(/Modelo Relacional Normalizado/i)).toBeInTheDocument();
    expect(screen.getByText(/AMBIENTE DETERMINÍSTICO/i)).toBeInTheDocument();

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    expect(codeTextarea).toHaveValue('CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY);');
  });

  it('deve alternar entre desafios ao clicar nas abas de exercícios', () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const ex2Btn = screen.getByRole('button', { name: /Exercício 2/i });
    fireEvent.click(ex2Btn);

    expect(screen.getByText(/Modelo de Documento Desnormalizado/i)).toBeInTheDocument();
    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    expect(codeTextarea).toHaveValue('{"usuario_id": 1, "contatos": []}');
  });

  it('deve executar validação offline ao clicar em "Validar Solução (Offline)"', async () => {
    vi.mocked(api.assessWithAi).mockResolvedValueOnce({
      status: 'APPROVED',
      feedback: 'Modelagem 3NF validada com sucesso pelo motor determinístico!',
      tradeOffAnalysis: 'Excelente separação de entidades relacionais.',
      efficiencyNotes: 'Execução local determinística sem dependências externas.',
      alternativeApproaches: ['Adicione índices em chaves estrangeiras'],
      modelUsed: 'Tutor Heurístico (Regras Locais)'
    });

    render(<LabWorkspace lab={mockLab} apiKey="" provider="heuristic" model="rules-engine-v1" />);

    const validateBtn = screen.getByRole('button', { name: /Validar Solução \(Offline\)/i });
    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, {
      target: { value: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));' }
    });

    fireEvent.click(validateBtn);

    await waitFor(() => {
      expect(api.assessWithAi).toHaveBeenCalledWith(
        expect.objectContaining({
          providerOverride: 'heuristic',
          labId: 'ddia-cap-03-lab-01',
          challengeId: 'lab-01-ch-1'
        })
      );
      expect(screen.getByText(/Modelagem 3NF validada com sucesso pelo motor determinístico!/i)).toBeInTheDocument();
      expect(screen.getByText(/PARECER: SOLUÇÃO APROVADA/i)).toBeInTheDocument();
    });
  });

  it('deve solicitar avaliação da IA e exibir parecer ao clicar em "Consultar Tutor IA"', async () => {
    vi.mocked(api.assessWithAi).mockResolvedValueOnce({
      status: 'APPROVED',
      feedback: 'Sua modelagem relacional segue rigorosamente a 3NF!',
      tradeOffAnalysis: 'Boa separação de entidades versus custo de joins.',
      efficiencyNotes: 'Executou com chave primária indexada.',
      alternativeApproaches: ['Tente adicionar restrição foreign key'],
      modelUsed: 'gemini-3.8-flash'
    });

    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const aiBtn = screen.getByRole('button', { name: /Consultar Tutor IA/i });
    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, {
      target: { value: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));' }
    });

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

  it('deve exibir aviso de autenticação pendente quando o Tutor IA retornar sem chave', async () => {
    vi.mocked(api.assessWithAi).mockResolvedValueOnce({
      status: 'NEEDS_REVISION',
      feedback: 'Chave de API não informada para o Google Gemini.',
      tradeOffAnalysis: '',
      efficiencyNotes: '',
      alternativeApproaches: [],
      modelUsed: 'Pending Authentication'
    });

    render(<LabWorkspace lab={mockLab} apiKey="" provider="gemini" model="gemini-3.8-flash" />);

    const aiBtn = screen.getByRole('button', { name: /Consultar Tutor IA/i });
    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, {
      target: { value: 'CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));' }
    });

    fireEvent.click(aiBtn);

    await waitFor(() => {
      expect(screen.getByText(/Chave de API Não Configurada/i)).toBeInTheDocument();
      expect(screen.getByText(/Chave de API não informada para o Google Gemini/i)).toBeInTheDocument();
    });
  });

  it('deve exibir o gabarito oficial de Martin Kleppmann ao clicar em "Conferir Gabarito de Trade-off"', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const reflectionInput = screen.getByPlaceholderText(/Digite aqui sua análise sobre os trade-offs/i);
    fireEvent.change(reflectionInput, { target: { value: 'Minha resposta sobre 3NF' } });

    const checkBtn = screen.getByRole('button', { name: /Conferir Gabarito de Trade-off/i });
    expect(checkBtn).not.toBeDisabled();

    fireEvent.click(checkBtn);

    expect(screen.getByTestId('model-reflection-box')).toBeInTheDocument();
    expect(screen.getByText(/Gabarito oficial de trade-off para o modelo 3NF/i)).toBeInTheDocument();
  });

  it('deve restaurar o template ao clicar em "Recarregar Template"', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const codeTextarea = screen.getByPlaceholderText(/-- Digite aqui sua instrução SQL ou Cypher.../i);
    fireEvent.change(codeTextarea, { target: { value: 'MODIFICAÇÃO TEMPORÁRIA' } });

    const reloadBtn = screen.getByRole('button', { name: /Recarregar Template/i });
    fireEvent.click(reloadBtn);

    await waitFor(() => {
      expect(api.resetChallengeSolution).toHaveBeenCalledWith('lab-01-ch-1');
      expect(codeTextarea).toHaveValue('CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY);');
    });
  });

  it('deve redefinir o desafio ao clicar no botão "Restaurar" com confirmação', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const resetBtn = screen.getByRole('button', { name: /^Restaurar$/i });
    fireEvent.click(resetBtn);

    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalled();
      expect(api.resetChallengeSolution).toHaveBeenCalledWith('lab-01-ch-1');
      expect(screen.getByText(/Esquema restaurado para o estado original com sucesso!/i)).toBeInTheDocument();
    });
  });

  it('deve alternar para a aba JSON e renderizar o payload estruturado', async () => {
    render(<LabWorkspace lab={mockLab} apiKey="test-key" provider="gemini" model="gemini-3.8-flash" />);

    const jsonTab = screen.getByRole('button', { name: /JSON/i });
    fireEvent.click(jsonTab);

    await waitFor(() => {
      expect(screen.getByText(/"labId": "ddia-cap-03-lab-01"/i)).toBeInTheDocument();
    });
  });
});
