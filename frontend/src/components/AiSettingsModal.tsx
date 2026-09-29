import React, { useState } from 'react';
import { X, Key, Cpu, Check, AlertCircle, Loader2, Zap, Sliders } from 'lucide-react';
import { testAiConnection } from '../services/api';
import type { AiTestConnectionResponse } from '../types';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  apiKey: string;
  onSaveApiKey: (key: string) => void;
  provider: string;
  onSaveProvider: (provider: string) => void;
  model: string;
  onSaveModel: (model: string) => void;
}

export const AiSettingsModal: React.FC<Props> = ({
  isOpen,
  onClose,
  apiKey,
  onSaveApiKey,
  provider,
  onSaveProvider,
  model,
  onSaveModel
}) => {
  const [tempKey, setTempKey] = useState(apiKey);
  const [tempProvider, setTempProvider] = useState(provider);
  const [tempModel, setTempModel] = useState(model || 'gemini-3.8-flash');
  const [saved, setSaved] = useState(false);

  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<AiTestConnectionResponse | null>(null);

  if (!isOpen) return null;

  const handleTest = async () => {
    setTesting(true);
    setTestResult(null);
    try {
      const res = await testAiConnection({
        provider: tempProvider,
        apiKey: tempKey,
        modelOverride: tempModel
      });
      setTestResult(res);
      if (res.valid && res.model) {
        setTempModel(res.model);
      }
    } catch (err: any) {
      setTestResult({
        valid: false,
        message: err.message || 'Erro ao contatar backend para teste.',
        model: null,
        latencyMs: 0
      });
    } finally {
      setTesting(false);
    }
  };

  const handleSave = () => {
    onSaveApiKey(tempKey);
    onSaveProvider(tempProvider);
    onSaveModel(tempModel);
    setSaved(true);
    setTimeout(() => {
      setSaved(false);
      onClose();
    }, 700);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 select-none">
      <div className="bg-[#fbf9f4] dark:bg-[#1c1a17] border-2 border-stone-900 dark:border-stone-600 max-w-lg w-full p-6 book-shadow-lg space-y-5 transition-colors">
        <div className="flex items-center justify-between border-b-2 border-stone-800 dark:border-stone-700 pb-3">
          <div className="flex items-center gap-2 text-stone-900 dark:text-stone-100 font-serif font-black text-lg">
            <Sliders className="w-5 h-5 text-[#8f1d1d] dark:text-[#df4444]" />
            <span>Configuração do Tutor de Inteligência Artificial</span>
          </div>
          <button
            onClick={onClose}
            className="text-stone-600 dark:text-stone-400 hover:text-stone-950 dark:hover:text-stone-100 p-1 border border-stone-400 dark:border-stone-700 bg-[#efebe1] dark:bg-[#252320] hover:bg-[#ded7c8] dark:hover:bg-[#2e2a26]"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="space-y-4 text-xs font-serif text-stone-800 dark:text-stone-200">
          <p className="leading-relaxed text-stone-700 dark:text-stone-300">
            Defina o provedor e a chave de acesso. O Tutor de IA avaliará a conformidade de suas consultas com os conceitos fundamentais de sistemas intensivos em dados.
          </p>

          <div className="space-y-1.5">
            <label className="block text-[10px] font-mono font-bold uppercase tracking-wider text-stone-700 dark:text-stone-400">
              Provedor de IA
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => {
                  setTempProvider('gemini');
                  setTestResult(null);
                }}
                className={`flex items-center justify-center gap-2 p-2.5 border-2 text-xs font-mono font-bold uppercase transition-all ${
                  tempProvider === 'gemini'
                    ? 'border-stone-900 dark:border-stone-500 bg-[#f7f4ec] dark:bg-[#272420] text-[#8f1d1d] dark:text-[#df4444] book-shadow-sm font-black'
                    : 'border-stone-400 dark:border-stone-700 bg-[#efebe1] dark:bg-[#1f1d1a] text-stone-600 dark:text-stone-400 hover:bg-[#e6e0d3] dark:hover:bg-[#292622]'
                }`}
              >
                <span>Google Gemini</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setTempProvider('ollama');
                  setTestResult(null);
                }}
                className={`flex items-center justify-center gap-2 p-2.5 border-2 text-xs font-mono font-bold uppercase transition-all ${
                  tempProvider === 'ollama'
                    ? 'border-stone-900 dark:border-stone-500 bg-[#f7f4ec] dark:bg-[#272420] text-[#8f1d1d] dark:text-[#df4444] book-shadow-sm font-black'
                    : 'border-stone-400 dark:border-stone-700 bg-[#efebe1] dark:bg-[#1f1d1a] text-stone-600 dark:text-stone-400 hover:bg-[#e6e0d3] dark:hover:bg-[#292622]'
                }`}
              >
                <Cpu className="w-3.5 h-3.5 text-stone-700 dark:text-stone-400" />
                <span>Ollama Local</span>
              </button>
            </div>
          </div>

          {tempProvider === 'gemini' ? (
            <div className="space-y-3">
              <div className="space-y-1">
                <label className="block text-[10px] font-mono font-bold uppercase tracking-wider text-stone-700 dark:text-stone-400">
                  Versão do Modelo
                </label>
                <select
                  value={tempModel}
                  onChange={(e) => {
                    setTempModel(e.target.value);
                    setTestResult(null);
                  }}
                  className="w-full bg-[#fdfcf9] dark:bg-[#141312] border-2 border-stone-700 dark:border-stone-600 px-3 py-2 text-stone-900 dark:text-stone-100 text-xs font-mono focus:outline-none focus:border-stone-950 dark:focus:border-stone-400"
                >
                  <option value="gemini-3.8-flash">Gemini 3.8 Flash (Recomendado - Mais Recente)</option>
                  <option value="gemini-3.5-flash-lite">Gemini 3.5 Flash-Lite (Ultrarrápido)</option>
                  <option value="gemini-2.5-flash">Gemini 2.5 Flash</option>
                </select>
              </div>

              <div className="space-y-1">
                <label className="block text-[10px] font-mono font-bold uppercase tracking-wider text-stone-700 dark:text-stone-400 flex items-center justify-between">
                  <span className="flex items-center gap-1.5">
                    <Key className="w-3.5 h-3.5 text-stone-600 dark:text-stone-400" />
                    <span>Gemini API Key</span>
                  </span>
                  <a
                    href="https://aistudio.google.com/app/apikey"
                    target="_blank"
                    rel="noreferrer"
                    className="text-[10px] font-serif italic text-[#8f1d1d] dark:text-[#df4444] hover:underline"
                  >
                    Obter chave gratuita no Google AI Studio &rarr;
                  </a>
                </label>

                <div className="flex gap-2">
                  <input
                    type="password"
                    value={tempKey}
                    onChange={(e) => {
                      setTempKey(e.target.value);
                      setTestResult(null);
                    }}
                    placeholder="Cole sua API Key (AIzaSy...)"
                    className="flex-1 bg-[#fdfcf9] dark:bg-[#141312] border-2 border-stone-700 dark:border-stone-600 px-3 py-2 text-stone-950 dark:text-stone-100 placeholder-stone-400 dark:placeholder-stone-600 focus:outline-none focus:border-stone-900 dark:focus:border-stone-400 text-xs font-mono"
                  />
                  <button
                    type="button"
                    onClick={handleTest}
                    disabled={testing || !tempKey.trim()}
                    className="flex items-center gap-1.5 px-3 py-2 bg-[#eee8db] dark:bg-[#282622] hover:bg-[#ded7c8] dark:hover:bg-[#33302b] border-2 border-stone-800 dark:border-stone-600 text-stone-900 dark:text-stone-100 text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all disabled:opacity-40"
                  >
                    {testing ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <Zap className="w-3.5 h-3.5 text-[#8f1d1d] dark:text-[#df4444]" />
                    )}
                    <span>{testing ? 'Testando...' : 'Testar'}</span>
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-2">
              <div className="p-3 bg-[#f5f0e4] dark:bg-[#1f1d1a] border border-stone-400 dark:border-stone-700 text-xs font-serif text-stone-800 dark:text-stone-300">
                Ollama deve estar ativo localmente em <code className="font-mono font-bold">http://localhost:11434</code> com o modelo <code className="font-mono font-bold">qwen2.5-coder:1.5b</code>.
              </div>
              <button
                type="button"
                onClick={handleTest}
                disabled={testing}
                className="w-full flex items-center justify-center gap-1.5 px-3 py-2 bg-[#eee8db] dark:bg-[#282622] hover:bg-[#ded7c8] dark:hover:bg-[#33302b] border-2 border-stone-800 dark:border-stone-600 text-stone-900 dark:text-stone-100 text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all"
              >
                {testing ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Zap className="w-3.5 h-3.5 text-[#8f1d1d] dark:text-[#df4444]" />
                )}
                <span>Testar Conexão com Ollama</span>
              </button>
            </div>
          )}

          {testResult && (
            <div
              className={`p-3 border-2 text-xs space-y-1 font-mono ${
                testResult.valid
                  ? 'bg-[#dcfce7] dark:bg-[#152e18] border-[#166534] dark:border-[#15803d] text-[#14532d] dark:text-[#86efac]'
                  : 'bg-[#fee2e2] dark:bg-[#381616] border-[#b91c1c] text-[#7f1d1d] dark:text-[#fca5a5]'
              }`}
            >
              <div className="flex items-center gap-2 font-bold uppercase">
                {testResult.valid ? (
                  <Check className="w-4 h-4 text-[#15803d] dark:text-[#4ade80]" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-[#b91c1c] dark:text-[#f87171]" />
                )}
                <span>{testResult.valid ? 'CHAVE VALIDADA COM SUCESSO' : 'FALHA NA VALIDAÇÃO'}</span>
              </div>
              <p className="font-serif leading-relaxed text-stone-900 dark:text-stone-200">{testResult.message}</p>
              {testResult.model && (
                <div className="text-[10px] text-stone-600 dark:text-stone-400 pt-0.5">
                  Versão ativa: <strong>{testResult.model}</strong> ({testResult.latencyMs}ms)
                </div>
              )}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-3 border-t-2 border-stone-800 dark:border-stone-700">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-stone-400 dark:border-stone-600 bg-[#eee8db] dark:bg-[#252320] hover:bg-[#ded7c8] dark:hover:bg-[#302c28] text-xs font-mono font-bold uppercase text-stone-800 dark:text-stone-300"
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            className="flex items-center gap-1.5 px-4 py-2 bg-[#8f1d1d] hover:bg-[#771818] dark:bg-[#991b1b] dark:hover:bg-[#7f1d1d] border-2 border-stone-950 dark:border-stone-600 text-white text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all"
          >
            {saved ? <Check className="w-4 h-4" /> : null}
            <span>{saved ? 'Salvo!' : 'Salvar Configurações'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
