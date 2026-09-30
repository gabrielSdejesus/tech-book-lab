import { useState, useEffect } from 'react';
import type { Book, Lab, InfraStatus } from './types';
import type { Theme } from './utils/theme';
import { getInitialTheme, toggleTheme, applyTheme } from './utils/theme';
import { getBooks, getInfraStatus } from './services/api';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { Bookshelf } from './components/Bookshelf';
import { LabWorkspace } from './components/LabWorkspace';
import { AiSettingsModal } from './components/AiSettingsModal';
import { Loader2, AlertCircle } from 'lucide-react';
import { LanguageProvider, useLanguage } from './i18n/LanguageContext';

export function App() {
  return (
    <LanguageProvider>
      <AppContent />
    </LanguageProvider>
  );
}

function AppContent() {
  const { locale } = useLanguage();
  const [theme, setTheme] = useState<Theme>(getInitialTheme);

  const [books, setBooks] = useState<Book[]>([]);
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const [selectedLab, setSelectedLab] = useState<Lab | null>(null);
  const [currentView, setCurrentView] = useState<'bookshelf' | 'workspace'>('bookshelf');
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

  const handleSelectBook = (book: Book) => {
    setSelectedBook(book);
    if (book.chapters && book.chapters.length > 0 && book.chapters[0].labs && book.chapters[0].labs.length > 0) {
      setSelectedLab(book.chapters[0].labs[0]);
    } else {
      setSelectedLab(null);
    }
    setCurrentView('workspace');
  };

  useEffect(() => {
    async function init() {
      try {
        const [loadedBooks, status] = await Promise.all([
          getBooks(),
          getInfraStatus().catch(() => null),
        ]);
        setBooks(loadedBooks);
        if (status) {
          setInfraStatus(status);
        }
      } catch (err: any) {
        setError(err.message || 'Erro ao carregar dados do catálogo');
      } finally {
        setLoading(false);
      }
    }

    void init();

    const interval = setInterval(refreshInfra, 12000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-[#fbf9f4] dark:bg-[#141312] flex flex-col items-center justify-center text-stone-800 dark:text-stone-200 gap-3 font-serif">
        <Loader2 className="w-8 h-8 animate-spin text-[#8f1d1d] dark:text-[#df4444]" />
        <span className="text-sm font-bold tracking-tight">
          {locale === 'pt' ? 'Carregando Caderno de Laboratório...' : 'Loading Lab Notebook...'}
        </span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[#fbf9f4] dark:bg-[#141312] flex flex-col items-center justify-center p-6 text-center font-serif">
        <div className="max-w-md p-6 bg-[#f7f4ec] dark:bg-[#1a1917] border-2 border-stone-800 dark:border-stone-700 book-shadow-lg space-y-4">
          <AlertCircle className="w-10 h-10 text-[#8f1d1d] dark:text-[#df4444] mx-auto" />
          <h2 className="text-lg font-bold text-stone-900 dark:text-stone-100">
            {locale === 'pt' ? 'Falha ao Conectar ao Servidor Backend' : 'Failed to Connect to Backend Server'}
          </h2>
          <p className="text-xs text-stone-700 dark:text-stone-300 leading-relaxed font-serif">
            {locale === 'pt'
              ? 'Certifique-se de que a API Java Spring Boot está em execução na porta 8080.'
              : 'Make sure the Java Spring Boot API is running on port 8080.'}
          </p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-[#8f1d1d] hover:bg-[#771818] border-2 border-stone-900 dark:border-stone-600 text-white text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed"
          >
            {locale === 'pt' ? 'Tentar Novamente' : 'Try Again'}
          </button>
        </div>
      </div>
    );

  }

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
        isBookshelfActive={currentView === 'bookshelf'}
        onNavigateBookshelf={() => setCurrentView('bookshelf')}
      />

      {currentView === 'bookshelf' ? (
        <Bookshelf books={books} onSelectBook={handleSelectBook} />
      ) : (
        <div className="flex-1 flex overflow-hidden">
          <Sidebar
            books={selectedBook ? [selectedBook] : books}
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
              {locale === 'pt'
                ? 'Selecione uma seção ou exercício na tábua de matérias ao lado para iniciar.'
                : 'Select a section or exercise from the table of contents to begin.'}
            </div>

          )}
        </div>
      )}

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
