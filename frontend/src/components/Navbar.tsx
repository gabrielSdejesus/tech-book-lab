import React from 'react';
import { BookOpen, Sliders, Moon, Sun, Library } from 'lucide-react';
import type { Theme } from '../utils/theme';
import { useLanguage } from '../i18n/LanguageContext';

interface Props {
  onOpenSettings: () => void;
  selectedBookTitle: string;
  theme: Theme;
  onToggleTheme: () => void;
  isBookshelfActive?: boolean;
  onNavigateBookshelf?: () => void;
  isSidebarOpen?: boolean;
  onToggleSidebar?: () => void;
}

export const Navbar: React.FC<Props> = ({
  onOpenSettings,
  selectedBookTitle,
  theme,
  onToggleTheme,
  isBookshelfActive = false,
  onNavigateBookshelf,
  isSidebarOpen = true,
  onToggleSidebar,
}) => {
  const { locale, setLocale, t } = useLanguage();

  return (
    <header className="h-18 border-b-2 border-stone-800 dark:border-stone-700 bg-[#f7f4ec] dark:bg-[#1a1917] px-6 flex items-center justify-between sticky top-0 z-40 select-none transition-colors">
      {/* Brand & Masthead */}
      <div className="flex items-center gap-4">
        {!isBookshelfActive && onToggleSidebar ? (
          <button
            onClick={onToggleSidebar}
            aria-label={t.sidebar.toggleSidebar}
            aria-expanded={isSidebarOpen}
            title={isSidebarOpen ? t.sidebar.collapseSidebar : t.sidebar.expandSidebar}
            className="w-10 h-10 border-2 border-stone-800 dark:border-stone-600 bg-[#8f1d1d] hover:bg-[#771818] flex items-center justify-center text-white book-shadow-sm book-shadow-pressed transition-all"
          >
            <BookOpen className="w-5 h-5 stroke-[2.2]" />
          </button>
        ) : (
          <div
            className={`w-10 h-10 border-2 border-stone-800 dark:border-stone-600 bg-[#8f1d1d] flex items-center justify-center text-white book-shadow-sm ${
              onNavigateBookshelf ? 'cursor-pointer hover:scale-105 transition-transform' : ''
            }`}
            onClick={onNavigateBookshelf}
            title={onNavigateBookshelf ? 'Ir para a Estante de Livros' : undefined}
          >
            <BookOpen className="w-5 h-5 stroke-[2.2]" />
          </div>
        )}

        <div
          role={onNavigateBookshelf ? 'button' : undefined}
          tabIndex={onNavigateBookshelf ? 0 : undefined}
          aria-label={onNavigateBookshelf ? (t.common.goToBookshelf || 'Ir para a Estante de Livros') : undefined}
          className={`${onNavigateBookshelf ? 'cursor-pointer group focus-visible:outline-2 focus-visible:outline-[#8f1d1d] focus-visible:outline-offset-2 dark:focus-visible:outline-[#df4444]' : ''}`}
          onClick={onNavigateBookshelf}
          onKeyDown={(e) => {
            if (onNavigateBookshelf && (e.key === 'Enter' || e.key === ' ')) {
              e.preventDefault();
              onNavigateBookshelf();
            }
          }}
          title={onNavigateBookshelf ? (t.common.goToBookshelf || 'Ir para a Estante de Livros') : undefined}
        >
          <div className="flex items-center gap-2">
            <h1 className="text-lg font-serif font-black tracking-tight text-stone-900 dark:text-stone-100 uppercase group-hover:text-[#8f1d1d] dark:group-hover:text-[#df4444] transition-colors">
              Tech Book <span className="text-[#8f1d1d] dark:text-[#df4444]">Lab</span>
            </h1>
            <span className="text-[10px] font-mono uppercase px-1.5 py-0.5 border border-stone-700 dark:border-stone-600 bg-[#ece7db] dark:bg-[#272522] text-stone-800 dark:text-stone-300 font-semibold tracking-wide">
              TBL
            </span>
          </div>
          <p className="text-xs font-serif italic text-stone-600 dark:text-stone-400">
            Caderno de Estudos Práticos &bull; {isBookshelfActive ? 'Catálogo Geral' : selectedBookTitle}
          </p>
        </div>
      </div>

      {/* Hardware / Engine Status & Controls */}
      <div className="flex items-center gap-3">
        {/* Bookshelf Navigation Button (when inside workspace) */}
        {!isBookshelfActive && onNavigateBookshelf && (
          <button
            onClick={onNavigateBookshelf}
            title={t.common.returnToBookshelf}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-[#efebe1] dark:bg-[#23211e] hover:bg-[#e4dfd3] dark:hover:bg-[#2e2b27] border-2 border-stone-800 dark:border-stone-700 text-stone-900 dark:text-stone-200 text-xs font-mono font-bold tracking-tight transition-all book-shadow-sm book-shadow-pressed"
          >
            <Library className="w-3.5 h-3.5 text-[#8f1d1d] dark:text-[#df4444]" />
            <span>{t.common.bookshelf.toUpperCase()}</span>
          </button>
        )}
        {/* Theme Toggle Button */}
        <button
          onClick={onToggleTheme}
          title={t.common.toggleTheme}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-[#efebe1] dark:bg-[#23211e] hover:bg-[#e4dfd3] dark:hover:bg-[#2e2b27] border-2 border-stone-800 dark:border-stone-700 text-stone-900 dark:text-stone-200 text-xs font-mono font-bold tracking-tight transition-all book-shadow-sm book-shadow-pressed"
        >
          {theme === 'dark' ? (
            <>
              <Moon className="w-3.5 h-3.5 text-amber-400" />
              <span>{locale === 'pt' ? 'ESCURO' : 'DARK'}</span>
            </>
          ) : (
            <>
              <Sun className="w-3.5 h-3.5 text-amber-600" />
              <span>{locale === 'pt' ? 'CLARO' : 'LIGHT'}</span>
            </>
          )}
        </button>

        {/* Language Selector PT | EN */}
        <div className="flex items-center bg-[#efebe1] dark:bg-[#23211e] border-2 border-stone-800 dark:border-stone-700 text-xs font-mono font-bold book-shadow-sm overflow-hidden">
          <button
            onClick={() => setLocale('pt')}
            className={`px-2.5 py-1.5 transition-colors ${
              locale === 'pt'
                ? 'bg-[#8f1d1d] text-white font-black'
                : 'text-stone-700 dark:text-stone-300 hover:bg-[#e4dfd3] dark:hover:bg-[#2e2b27]'
            }`}
            title="Mudar idioma para Português (Brasil)"
            aria-label="PT"
          >
            PT
          </button>
          <div className="w-[1px] h-4 bg-stone-800 dark:bg-stone-700" />
          <button
            onClick={() => setLocale('en')}
            className={`px-2.5 py-1.5 transition-colors ${
              locale === 'en'
                ? 'bg-[#8f1d1d] text-white font-black'
                : 'text-stone-700 dark:text-stone-300 hover:bg-[#e4dfd3] dark:hover:bg-[#2e2b27]'
            }`}
            title="Switch language to English"
            aria-label="EN"
          >
            EN
          </button>
        </div>

        {/* AI Tutor Config Button */}
        <button
          onClick={onOpenSettings}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-[#fcfbf9] dark:bg-[#1f1d1a] hover:bg-[#efebe1] dark:hover:bg-[#292622] border-2 border-stone-800 dark:border-stone-700 text-stone-900 dark:text-stone-100 text-xs font-mono font-bold tracking-tight transition-all book-shadow-sm book-shadow-pressed"
        >
          <Sliders className="w-3.5 h-3.5 text-[#8f1d1d] dark:text-[#df4444]" />
          <span>{t.common.configureAi}</span>
        </button>
      </div>
    </header>
  );
};

