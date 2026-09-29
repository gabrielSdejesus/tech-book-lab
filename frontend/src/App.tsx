import { useState, useEffect } from 'react';
import type { Book, Lab, InfraStatus } from './types';
import type { Theme } from './utils/theme';
import { getInitialTheme, toggleTheme, applyTheme } from './utils/theme';
import { getBooks, getInfraStatus } from './services/api';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { LabWorkspace } from './components/LabWorkspace';
import { AiSettingsModal } from './components/AiSettingsModal';
import { Loader2, AlertCircle } from 'lucide-react';

export function App() {
  const [theme, setTheme] = useState<Theme>(getInitialTheme);
  const [books, setBooks] = useState<Book[]>([]);
  const [selectedLab, setSelectedLab] = useState<Lab | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Apply theme to document element
  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  // Infra state
  const [infraStatus, setInfraStatus] = useState<InfraStatus | null>(null);
  const [loadingInfra, setLoadingInfra] = useState(false);

  // AI settings
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [apiKey, setApiKey] = useState<string>(() => localStorage.getItem('gemini_api_key') || '');
  const [provider, setProvider] = useState<string>(() => localStorage.getItem('ai_provider') || 'gemini');
  const [model, setModel] = useState<string>(() => localStorage.getItem('gemini_model') || 'gemini-3.8-flash');

  const handleSaveApiKey = (key: string) => {
    setApiKey(key);
    localStorage.setItem('gemini_api_key', key);
  };

  const handleSaveProvider = (prov: string) => {
    setProvider(prov);
    localStorage.setItem('ai_provider', prov);
  };

  const handleSaveModel = (mod: string) => {
    setModel(mod);
    localStorage.setItem('gemini_model', mod);
  };

  const refreshInfra = async () => {
    setLoadingInfra(true);
    try {
      const status = await getInfraStatus();
      setInfraStatus(status);
    } catch {
      // Ignora erro de rede temporário
    } finally {
      setLoadingInfra(false);
    }
  };

  useEffect(() => {
    async function init() {
      try {
        const loadedBooks = await getBooks();
        setBooks(loadedBooks);
        if (loadedBooks.length > 0 && loadedBooks[0].chapters.length > 0 && loadedBooks[0].chapters[0].labs.length > 0) {
          setSelectedLab(loadedBooks[0].chapters[0].labs[0]);
        }
      } catch (err: any) {
        setError(err.message || 'Erro ao carregar dados do catálogo');
      } finally {
        setLoading(false);
      }
    }

    init();
    refreshInfra();

    const interval = setInterval(refreshInfra, 12000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-[#fbf9f4] dark:bg-[#141312] flex flex-col items-center justify-center text-stone-800 dark:text-stone-200 gap-3 font-serif">
        <Loader2 className="w-8 h-8 animate-spin text-[#8f1d1d] dark:text-[#df4444]" />
        <span className="text-sm font-bold tracking-tight">Carregando Caderno de Laboratório...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[#fbf9f4] dark:bg-[#141312] flex flex-col items-center justify-center p-6 text-center font-serif">
        <div className="max-w-md p-6 bg-[#f7f4ec] dark:bg-[#1a1917] border-2 border-stone-800 dark:border-stone-700 book-shadow-lg space-y-4">
          <AlertCircle className="w-10 h-10 text-[#8f1d1d] dark:text-[#df4444] mx-auto" />
          <h2 className="text-lg font-bold text-stone-900 dark:text-stone-100">Falha ao Conectar ao Servidor Backend</h2>
          <p className="text-xs text-stone-700 dark:text-stone-300 leading-relaxed font-serif">
            Certifique-se de que a API Java Spring Boot está em execução na porta 8080.
          </p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-[#8f1d1d] hover:bg-[#771818] border-2 border-stone-900 dark:border-stone-600 text-white text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed"
          >
            Tentar Novamente
          </button>
        </div>
      </div>
    );
  }

  const selectedBook = books[0];

  return (
    <div className="min-h-screen bg-[#fbf9f4] dark:bg-[#141312] text-stone-900 dark:text-stone-100 flex flex-col font-serif">
      <Navbar
        infraStatus={infraStatus}
        loadingInfra={loadingInfra}
        onRefreshInfra={refreshInfra}
        onOpenSettings={() => setIsSettingsOpen(true)}
        selectedBookTitle={selectedBook?.title || 'Data-Intensive Labs'}
        theme={theme}
        onToggleTheme={() => setTheme((prev) => toggleTheme(prev))}
      />

      <div className="flex-1 flex overflow-hidden">
        <Sidebar
          books={books}
          selectedLab={selectedLab}
          onSelectLab={(lab) => setSelectedLab(lab)}
        />

        {selectedLab ? (
          <LabWorkspace
            key={selectedLab.id}
            lab={selectedLab}
            apiKey={apiKey}
            provider={provider}
            model={model}
          />
        ) : (
          <div className="flex-1 flex items-center justify-center text-stone-500 dark:text-stone-400 text-sm font-serif italic">
            Selecione uma seção ou exercício na tábua de matérias ao lado para iniciar.
          </div>
        )}
      </div>

      <AiSettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        apiKey={apiKey}
        onSaveApiKey={handleSaveApiKey}
        provider={provider}
        onSaveProvider={handleSaveProvider}
        model={model}
        onSaveModel={handleSaveModel}
      />
    </div>
  );
}

export default App;
