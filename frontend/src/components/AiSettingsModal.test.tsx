import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AiSettingsModal } from './AiSettingsModal';
import * as api from '../services/api';
import { LanguageProvider } from '../i18n/LanguageContext';

vi.mock('../services/api', () => ({
  testAiConnection: vi.fn(),
  getBooks: vi.fn(),
  executeQuery: vi.fn(),
  resetLab: vi.fn(),
  assessWithAi: vi.fn(),
  getAiProviders: vi.fn(),
}));

describe('AiSettingsModal Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('não deve renderizar nada quando isOpen for false', () => {
    const { container } = render(
      <AiSettingsModal
        isOpen={false}
        onClose={vi.fn()}
        apiKey=""
        onSaveApiKey={vi.fn()}
        provider="gemini"
        onSaveProvider={vi.fn()}
        model="gemini-3.8-flash"
        onSaveModel={vi.fn()}
      />
    );

    expect(container.firstChild).toBeNull();
  });

  it('deve renderizar os campos quando isOpen for true', () => {
    render(
      <AiSettingsModal
        isOpen={true}
        onClose={vi.fn()}
        apiKey="test-key-123"
        onSaveApiKey={vi.fn()}
        provider="gemini"
        onSaveProvider={vi.fn()}
        model="gemini-3.8-flash"
        onSaveModel={vi.fn()}
      />
    );

    expect(screen.getByText(/Configuração do Tutor de Inteligência Artificial/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue('test-key-123')).toBeInTheDocument();
  });

  it('deve disparar teste de conexão e exibir sucesso', async () => {
    vi.mocked(api.testAiConnection).mockResolvedValueOnce({
      valid: true,
      message: 'Autenticação bem-sucedida!',
      model: 'gemini-3.8-flash',
      latencyMs: 120
    });

    render(
      <AiSettingsModal
        isOpen={true}
        onClose={vi.fn()}
        apiKey="test-key"
        onSaveApiKey={vi.fn()}
        provider="gemini"
        onSaveProvider={vi.fn()}
        model="gemini-3.8-flash"
        onSaveModel={vi.fn()}
      />
    );

    const testBtn = screen.getByRole('button', { name: /Testar/i });
    fireEvent.click(testBtn);

    await waitFor(() => {
      expect(screen.getByText(/Autenticação bem-sucedida!/i)).toBeInTheDocument();
      expect(screen.getByText(/120ms/i)).toBeInTheDocument();
    });
  });

  it('deve salvar configurações ao clicar no botão salvar', async () => {
    const handleSaveKey = vi.fn();
    const handleSaveProv = vi.fn();
    const handleSaveMod = vi.fn();
    const handleClose = vi.fn();

    render(
      <AiSettingsModal
        isOpen={true}
        onClose={handleClose}
        apiKey="chave-inicial"
        onSaveApiKey={handleSaveKey}
        provider="gemini"
        onSaveProvider={handleSaveProv}
        model="gemini-3.8-flash"
        onSaveModel={handleSaveMod}
      />
    );

    const saveBtn = screen.getByRole('button', { name: /Salvar Configurações/i });
    fireEvent.click(saveBtn);

    expect(handleSaveKey).toHaveBeenCalledWith('chave-inicial');
    expect(handleSaveProv).toHaveBeenCalledWith('gemini');
    expect(handleSaveMod).toHaveBeenCalledWith('gemini-3.8-flash');

    await waitFor(() => {
      expect(handleClose).toHaveBeenCalled();
    }, { timeout: 1500 });
  });

  it('deve respeitar e preservar o modelo selecionado pelo usuário mesmo após teste de conexão', async () => {
    vi.mocked(api.testAiConnection).mockResolvedValueOnce({
      valid: true,
      message: 'Conexão validada',
      model: 'gemini-3.8-flash', // backend retornando modelo diferente ou fallback
      latencyMs: 90
    });

    const handleSaveModel = vi.fn();

    render(
      <AiSettingsModal
        isOpen={true}
        onClose={vi.fn()}
        apiKey="test-key"
        onSaveApiKey={vi.fn()}
        provider="gemini"
        onSaveProvider={vi.fn()}
        model="gemini-3.8-flash"
        onSaveModel={handleSaveModel}
      />
    );

    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'gemini-2.5-flash' } });
    expect(select).toHaveValue('gemini-2.5-flash');

    const testBtn = screen.getByRole('button', { name: /Testar/i });
    fireEvent.click(testBtn);

    await waitFor(() => {
      expect(api.testAiConnection).toHaveBeenCalledWith({
        provider: 'gemini',
        apiKey: 'test-key',
        modelOverride: 'gemini-2.5-flash'
      });
    });

    // O valor do select não deve ser revertido para o modelo retornado pelo teste
    expect(select).toHaveValue('gemini-2.5-flash');

    const saveBtn = screen.getByRole('button', { name: /Salvar Configurações/i });
    fireEvent.click(saveBtn);

    expect(handleSaveModel).toHaveBeenCalledWith('gemini-2.5-flash');
  });

  it('deve possuir classe disabled:cursor-not-allowed e disabled no botão de teste quando a chave API estiver vazia', () => {
    render(
      <AiSettingsModal
        isOpen={true}
        onClose={vi.fn()}
        apiKey=""
        onSaveApiKey={vi.fn()}
        provider="gemini"
        onSaveProvider={vi.fn()}
        model="gemini-3.8-flash"
        onSaveModel={vi.fn()}
      />
    );

    const testBtn = screen.getByRole('button', { name: /Testar/i });
    expect(testBtn).toBeDisabled();
    expect(testBtn).toHaveClass('disabled:cursor-not-allowed');
  });

  it('deve carregar provedores dinamicamente via getAiProviders e renderizar opções de modelos e provedores', async () => {
    vi.mocked(api.getAiProviders).mockResolvedValueOnce([
      {
        id: 'gemini',
        name: 'Google Gemini',
        description: 'Gemini desc',
        requiresApiKey: true,
        apiKeyPlaceholder: 'Cole sua API Key...',
        helpUrl: 'https://aistudio.google.com/',
        defaultModel: 'gemini-2.5-flash',
        models: [
          { id: 'gemini-2.5-flash', name: 'Gemini 2.5 Flash', recommended: true }
        ]
      },
      {
        id: 'deepseek',
        name: 'DeepSeek AI',
        description: 'DeepSeek LLM API',
        requiresApiKey: true,
        apiKeyPlaceholder: 'sk-deepseek-...',
        helpUrl: 'https://deepseek.com',
        defaultModel: 'deepseek-chat',
        models: [
          { id: 'deepseek-chat', name: 'DeepSeek V3', recommended: true }
        ]
      }
    ]);

    render(
      <AiSettingsModal
        isOpen={true}
        onClose={vi.fn()}
        apiKey="minha-chave"
        onSaveApiKey={vi.fn()}
        provider="gemini"
        onSaveProvider={vi.fn()}
        model="gemini-2.5-flash"
        onSaveModel={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /DeepSeek AI/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /DeepSeek AI/i }));

    expect(screen.getByText('DeepSeek LLM API')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('sk-deepseek-...')).toBeInTheDocument();
    expect(screen.getByText(/DeepSeek V3/i)).toBeInTheDocument();
  });

  it('deve renderizar descrições dos provedores em português por padrão e ao alternar entre Gemini e Ollama', async () => {
    localStorage.setItem('tbl_locale', 'pt');
    render(
      <LanguageProvider>
        <AiSettingsModal
          isOpen={true}
          onClose={vi.fn()}
          apiKey="test-key"
          onSaveApiKey={vi.fn()}
          provider="gemini"
          onSaveProvider={vi.fn()}
          model="gemini-2.5-flash"
          onSaveModel={vi.fn()}
        />
      </LanguageProvider>
    );

    expect(screen.getByText(/Modelos de linguagem do Google AI Studio/i)).toBeInTheDocument();

    const ollamaBtn = screen.getByRole('button', { name: /Ollama Local/i });
    fireEvent.click(ollamaBtn);

    expect(screen.getByText(/Execução local e privada via Ollama/i)).toBeInTheDocument();
    localStorage.removeItem('tbl_locale');
  });

  it('deve renderizar descrições dos provedores em inglês quando o idioma for en e alternar entre Gemini e Ollama', async () => {
    localStorage.setItem('tbl_locale', 'en');
    render(
      <LanguageProvider>
        <AiSettingsModal
          isOpen={true}
          onClose={vi.fn()}
          apiKey="test-key"
          onSaveApiKey={vi.fn()}
          provider="gemini"
          onSaveProvider={vi.fn()}
          model="gemini-2.5-flash"
          onSaveModel={vi.fn()}
        />
      </LanguageProvider>
    );

    expect(screen.getByText(/Language models from Google AI Studio/i)).toBeInTheDocument();

    const ollamaBtn = screen.getByRole('button', { name: /Ollama Local/i });
    fireEvent.click(ollamaBtn);

    expect(screen.getByText(/Local and private execution via Ollama/i)).toBeInTheDocument();
    localStorage.removeItem('tbl_locale');
  });
});

