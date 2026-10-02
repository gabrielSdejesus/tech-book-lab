import React, { useState, useEffect } from 'react';
import { X, Key, Cpu, Check, AlertCircle, Loader2, Zap, Sliders } from 'lucide-react';
import { testAiConnection, getAiProviders } from '../services/api';
import type { AiTestConnectionResponse, AiProviderInfo } from '../types';
import { useLanguage } from '../i18n/LanguageContext';
import type { TranslationSchema } from '../i18n/types';

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

const getDefaultProviders = (t: TranslationSchema): AiProviderInfo[] => [
  {
    id: 'gemini',
    name: 'Google Gemini',
    description: t.aiModal.geminiDescription,
    requiresApiKey: true,
    apiKeyPlaceholder: 'AIzaSy...',
    helpUrl: 'https://aistudio.google.com/app/apikey',
    defaultModel: 'gemini-2.5-flash',
    models: [
      { id: 'gemini-2.5-flash', name: 'Gemini 2.5 Flash', recommended: true },
      { id: 'gemini-2.0-flash', name: 'Gemini 2.0 Flash', recommended: false },
      { id: 'gemini-1.5-flash', name: 'Gemini 1.5 Flash', recommended: false },
      { id: 'gemini-1.5-pro', name: 'Gemini 1.5 Pro', recommended: false },
      { id: 'gemini-3.8-flash', name: 'Gemini 3.8 Flash', recommended: false },
      { id: 'gemini-3.5-flash-lite', name: 'Gemini 3.5 Flash-Lite', recommended: false },
    ],
  },
  {
    id: 'ollama',
    name: 'Ollama Local',
    description: t.aiModal.ollamaDescription,
    requiresApiKey: false,
    apiKeyPlaceholder: '',
    helpUrl: 'https://ollama.com/',
    defaultModel: 'qwen2.5-coder:1.5b',
    models: [],
  },
];

export const AiSettingsModal: React.FC<Props> = ({
  isOpen,
  onClose,
  apiKey,
  onSaveApiKey,
  provider,
  onSaveProvider,
  model,
  onSaveModel,
}) => {
  const { t, locale } = useLanguage();
  const [customProviders, setCustomProviders] = useState<AiProviderInfo[] | null>(null);
  const [tempKey, setTempKey] = useState(apiKey);
  const [tempProvider, setTempProvider] = useState(provider || 'gemini');
  const [tempModel, setTempModel] = useState(model || 'gemini-2.5-flash');
  const [saved, setSaved] = useState(false);

  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<AiTestConnectionResponse | null>(null);

  const [prevProps, setPrevProps] = useState({ isOpen, apiKey, provider, model });
  if (isOpen && (!prevProps.isOpen || prevProps.apiKey !== apiKey || prevProps.provider !== provider || prevProps.model !== model)) {
    setPrevProps({ isOpen, apiKey, provider, model });
    setTempKey(apiKey);
    setTempProvider(provider || 'gemini');
    setTempModel(model || (provider === 'ollama' ? 'qwen2.5-coder:1.5b' : 'gemini-2.5-flash'));
    setTestResult(null);
  }

  useEffect(() => {
    if (isOpen) {
      Promise.resolve(getAiProviders(locale))
        .then((data) => {
          if (Array.isArray(data) && data.length > 0) {
            setCustomProviders(data);
          }
        })
        .catch(() => {
          // Keep default providers
        });
    }
  }, [isOpen, locale]);

  if (!isOpen) return null;

  const defaultProviders = getDefaultProviders(t);
  const providers = customProviders || defaultProviders;
  const currentProvider =
    providers.find((p) => p.id.toLowerCase() === tempProvider.toLowerCase()) ||
    providers[0] ||
    defaultProviders[0];

  const getProviderDescription = (p: AiProviderInfo) => {
    if (p.id === 'gemini') {
      return (p.description && !p.description.includes('Google AI Studio'))
        ? p.description
        : t.aiModal.geminiDescription;
    }
    if (p.id === 'ollama') {
      return (p.description && !p.description.includes('Ollama'))
        ? p.description
        : t.aiModal.ollamaDescription;
    }
    return p.description;
  };

  const handleSelectProvider = (p: AiProviderInfo) => {
    setTempProvider(p.id);
    if (p.models && p.models.length > 0) {
      const hasModel = p.models.some((m) => m.id === tempModel);
      if (!hasModel) {
        setTempModel(p.defaultModel || p.models[0].id);
      }
    } else {
      setTempModel(p.defaultModel || (p.id === 'ollama' ? 'qwen2.5-coder:1.5b' : ''));
    }
    setTestResult(null);
  };

  const handleTest = async () => {
    setTesting(true);
    setTestResult(null);
    try {
      const res = await testAiConnection({
        provider: tempProvider,
        apiKey: tempKey,
        modelOverride: tempModel,
      });
      setTestResult(res);
    } catch (err: any) {
      setTestResult({
        valid: false,
        message: err.message || t.aiModal.contactError,
        model: null,
        latencyMs: 0,
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

  const hasPresetModels = currentProvider.models && currentProvider.models.length > 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 select-none">
      <div className="bg-[#fbf9f4] dark:bg-[#1c1a17] border-2 border-stone-900 dark:border-stone-600 max-w-lg w-full p-6 book-shadow-lg space-y-5 transition-colors">
        <div className="flex items-center justify-between border-b-2 border-stone-800 dark:border-stone-700 pb-3">
          <div className="flex items-center gap-2 text-stone-900 dark:text-stone-100 font-serif font-black text-lg">
            <Sliders className="w-5 h-5 text-[#8f1d1d] dark:text-[#df4444]" />
            <span>{t.aiModal.title}</span>
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
            {t.aiModal.description}
          </p>

          <div className="space-y-1.5">
            <label className="block text-[10px] font-mono font-bold uppercase tracking-wider text-stone-700 dark:text-stone-400">
              {t.aiModal.providerLabel}
            </label>
            <div className="grid grid-cols-2 gap-2">
              {providers.map((p) => {
                const isSelected = tempProvider.toLowerCase() === p.id.toLowerCase();
                return (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => handleSelectProvider(p)}
                    className={`flex items-center justify-center gap-2 p-2.5 border-2 text-xs font-mono font-bold uppercase transition-all ${
                      isSelected
                        ? 'border-stone-900 dark:border-stone-500 bg-[#f7f4ec] dark:bg-[#272420] text-[#8f1d1d] dark:text-[#df4444] book-shadow-sm font-black'
                        : 'border-stone-400 dark:border-stone-700 bg-[#efebe1] dark:bg-[#1f1d1a] text-stone-600 dark:text-stone-400 hover:bg-[#e6e0d3] dark:hover:bg-[#292622]'
                    }`}
                  >
                    {p.id === 'ollama' && <Cpu className="w-3.5 h-3.5 text-stone-700 dark:text-stone-400" />}
                    <span>{p.name}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {getProviderDescription(currentProvider) && (
            <p className="text-[11px] leading-relaxed text-stone-600 dark:text-stone-400 italic">
              {getProviderDescription(currentProvider)}
            </p>
          )}

          <div className="space-y-3">
            <div className="space-y-1">
              <label className="block text-[10px] font-mono font-bold uppercase tracking-wider text-stone-700 dark:text-stone-400">
                {currentProvider.id === 'ollama' ? t.aiModal.ollamaModel : t.aiModal.modelVersion}
              </label>
              {hasPresetModels ? (
                <select
                  value={tempModel}
                  onChange={(e) => {
                    setTempModel(e.target.value);
                    setTestResult(null);
                  }}
                  className="w-full bg-[#fdfcf9] dark:bg-[#141312] border-2 border-stone-700 dark:border-stone-600 px-3 py-2 text-stone-900 dark:text-stone-100 text-xs font-mono focus:outline-none focus:border-stone-950 dark:focus:border-stone-400"
                >
                  {currentProvider.models.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.name} {m.recommended ? t.aiModal.recommendedLatest : ''}
                    </option>
                  ))}
                  {tempModel && !currentProvider.models.some((m) => m.id === tempModel) && (
                    <option value={tempModel}>{tempModel}</option>
                  )}
                </select>
              ) : (
                <input
                  type="text"
                  value={tempModel}
                  onChange={(e) => {
                    setTempModel(e.target.value);
                    setTestResult(null);
                  }}
                  placeholder="ex: qwen2.5-coder:1.5b, llama3.2"
                  className="w-full bg-[#fdfcf9] dark:bg-[#141312] border-2 border-stone-700 dark:border-stone-600 px-3 py-2 text-stone-900 dark:text-stone-100 text-xs font-mono focus:outline-none focus:border-stone-950 dark:focus:border-stone-400"
                />
              )}
            </div>

            {currentProvider.requiresApiKey ? (
              <div className="space-y-1">
                <label className="block text-[10px] font-mono font-bold uppercase tracking-wider text-stone-700 dark:text-stone-400 flex items-center justify-between">
                  <span className="flex items-center gap-1.5">
                    <Key className="w-3.5 h-3.5 text-stone-600 dark:text-stone-400" />
                    <span>{currentProvider.name} API Key</span>
                  </span>
                  {currentProvider.helpUrl && (
                    <a
                      href={currentProvider.helpUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-[10px] font-serif italic text-[#8f1d1d] dark:text-[#df4444] hover:underline"
                    >
                      {t.aiModal.getFreeKey}
                    </a>
                  )}
                </label>

                <div className="flex gap-2">
                  <input
                    type="password"
                    value={tempKey}
                    onChange={(e) => {
                      setTempKey(e.target.value);
                      setTestResult(null);
                    }}
                    placeholder={currentProvider.apiKeyPlaceholder || t.aiModal.apiKeyPlaceholder}
                    className="flex-1 bg-[#fdfcf9] dark:bg-[#141312] border-2 border-stone-700 dark:border-stone-600 px-3 py-2 text-stone-950 dark:text-stone-100 placeholder-stone-400 dark:placeholder-stone-600 focus:outline-none focus:border-stone-900 dark:focus:border-stone-400 text-xs font-mono"
                  />
                  <button
                    type="button"
                    onClick={handleTest}
                    disabled={testing || !tempKey.trim()}
                    className="flex items-center gap-1.5 px-3 py-2 bg-[#eee8db] dark:bg-[#282622] hover:bg-[#ded7c8] dark:hover:bg-[#33302b] border-2 border-stone-800 dark:border-stone-600 text-stone-900 dark:text-stone-100 text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed"
                  >
                    {testing ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <Zap className="w-3.5 h-3.5 text-[#8f1d1d] dark:text-[#df4444]" />
                    )}
                    <span>{testing ? t.aiModal.testing : t.aiModal.test}</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-3">
                <div className="p-3 bg-[#f5f0e4] dark:bg-[#1f1d1a] border border-stone-400 dark:border-stone-700 text-xs font-serif text-stone-800 dark:text-stone-300">
                  {currentProvider.id === 'ollama' ? t.aiModal.ollamaNotice : getProviderDescription(currentProvider)}
                </div>
                <button
                  type="button"
                  onClick={handleTest}
                  disabled={testing}
                  className="w-full flex items-center justify-center gap-1.5 px-3 py-2 bg-[#eee8db] dark:bg-[#282622] hover:bg-[#ded7c8] dark:hover:bg-[#33302b] border-2 border-stone-800 dark:border-stone-600 text-stone-900 dark:text-stone-100 text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed"
                >
                  {testing ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <Zap className="w-3.5 h-3.5 text-[#8f1d1d] dark:text-[#df4444]" />
                  )}
                  <span>{t.aiModal.testOllama}</span>
                </button>
              </div>
            )}
          </div>

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
                <span>{testResult.valid ? t.aiModal.keyValidatedSuccess : t.aiModal.validationFailed}</span>
              </div>
              <p className="font-serif leading-relaxed text-stone-900 dark:text-stone-200">{testResult.message}</p>
              {testResult.model && (
                <div className="text-[10px] text-stone-600 dark:text-stone-400 pt-0.5">
                  {t.aiModal.activeVersion(testResult.model, testResult.latencyMs)}
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
            {t.aiModal.cancel}
          </button>
          <button
            onClick={handleSave}
            className="flex items-center gap-1.5 px-4 py-2 bg-[#8f1d1d] hover:bg-[#771818] dark:bg-[#991b1b] dark:hover:bg-[#7f1d1d] border-2 border-stone-950 dark:border-stone-600 text-white text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all"
          >
            {saved ? <Check className="w-4 h-4" /> : null}
            <span>{saved ? t.aiModal.saved : t.aiModal.saveSettings}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
