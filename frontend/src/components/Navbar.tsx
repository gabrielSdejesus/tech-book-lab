import React from 'react';
import { RefreshCw, BookOpen, Sliders, Moon, Sun } from 'lucide-react';
import type { InfraStatus } from '../types';
import type { Theme } from '../utils/theme';

interface Props {
  infraStatus: InfraStatus | null;
  loadingInfra: boolean;
  onRefreshInfra: () => void;
  onOpenSettings: () => void;
  selectedBookTitle: string;
  theme: Theme;
  onToggleTheme: () => void;
}

export const Navbar: React.FC<Props> = ({
  infraStatus,
  loadingInfra,
  onRefreshInfra,
  onOpenSettings,
  selectedBookTitle,
  theme,
  onToggleTheme
}) => {
  return (
    <header className="h-18 border-b-2 border-stone-800 dark:border-stone-700 bg-[#f7f4ec] dark:bg-[#1a1917] px-6 flex items-center justify-between sticky top-0 z-40 select-none transition-colors">
      {/* Brand & Masthead */}
      <div className="flex items-center gap-4">
        <div className="w-10 h-10 border-2 border-stone-800 dark:border-stone-600 bg-[#8f1d1d] flex items-center justify-center text-white book-shadow-sm">
          <BookOpen className="w-5 h-5 stroke-[2.2]" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-lg font-serif font-black tracking-tight text-stone-900 dark:text-stone-100 uppercase">
              Data-Intensive <span className="text-[#8f1d1d] dark:text-[#df4444]">Laboratories</span>
            </h1>
            <span className="text-[10px] font-mono uppercase px-1.5 py-0.5 border border-stone-700 dark:border-stone-600 bg-[#ece7db] dark:bg-[#272522] text-stone-800 dark:text-stone-300 font-semibold tracking-wide">
              Vol. I
            </span>
          </div>
          <p className="text-xs font-serif italic text-stone-600 dark:text-stone-400">
            Caderno de Estudos Práticos &bull; {selectedBookTitle}
          </p>
        </div>
      </div>

      {/* Hardware / Engine Status & Controls */}
      <div className="flex items-center gap-3">
        {/* Infra Status Stamps */}
        <div className="flex items-center gap-2 bg-[#efebe1] dark:bg-[#23211e] border border-stone-800 dark:border-stone-700 px-3 py-1.5 text-xs font-mono book-shadow-sm">
          <span className="text-[10px] text-stone-500 dark:text-stone-400 uppercase tracking-widest font-sans font-bold mr-1">
            MOTORES:
          </span>

          <div className="flex items-center gap-1.5" title={infraStatus?.postgresMessage || 'Checando...'}>
            <span
              className={`w-2.5 h-2.5 border border-stone-800 dark:border-stone-600 ${
                infraStatus?.postgresReady ? 'bg-[#15803d]' : 'bg-[#b91c1c]'
              }`}
            />
            <span className="text-stone-800 dark:text-stone-200 font-bold">PG:5432</span>
          </div>

          <span className="text-stone-400 dark:text-stone-600 font-sans">|</span>

          <div className="flex items-center gap-1.5" title={infraStatus?.neo4jMessage || 'Checando...'}>
            <span
              className={`w-2.5 h-2.5 border border-stone-800 dark:border-stone-600 ${
                infraStatus?.neo4jReady ? 'bg-[#15803d]' : 'bg-[#b91c1c]'
              }`}
            />
            <span className="text-stone-800 dark:text-stone-200 font-bold">NEO4J:7687</span>
          </div>

          <button
            onClick={onRefreshInfra}
            disabled={loadingInfra}
            title="Sondar conectividade dos bancos de dados"
            className="text-stone-600 dark:text-stone-400 hover:text-stone-950 dark:hover:text-stone-100 p-0.5 rounded ml-1 transition-colors"
          >
            <RefreshCw className={`w-3 h-3 ${loadingInfra ? 'animate-spin text-[#8f1d1d]' : ''}`} />
          </button>
        </div>

        {/* Theme Toggle Button */}
        <button
          onClick={onToggleTheme}
          title={theme === 'dark' ? 'Mudar para tema claro (Papel Marfim)' : 'Mudar para tema escuro (Terminal Noturno)'}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-[#efebe1] dark:bg-[#23211e] hover:bg-[#e4dfd3] dark:hover:bg-[#2e2b27] border-2 border-stone-800 dark:border-stone-700 text-stone-900 dark:text-stone-200 text-xs font-mono font-bold tracking-tight transition-all book-shadow-sm book-shadow-pressed"
        >
          {theme === 'dark' ? (
            <>
              <Moon className="w-3.5 h-3.5 text-amber-400" />
              <span>ESCURO</span>
            </>
          ) : (
            <>
              <Sun className="w-3.5 h-3.5 text-amber-600" />
              <span>CLARO</span>
            </>
          )}
        </button>

        {/* AI Tutor Config Button */}
        <button
          onClick={onOpenSettings}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-[#fcfbf9] dark:bg-[#1f1d1a] hover:bg-[#efebe1] dark:hover:bg-[#292622] border-2 border-stone-800 dark:border-stone-700 text-stone-900 dark:text-stone-100 text-xs font-mono font-bold tracking-tight transition-all book-shadow-sm book-shadow-pressed"
        >
          <Sliders className="w-3.5 h-3.5 text-[#8f1d1d] dark:text-[#df4444]" />
          <span>CONFIGURAR TUTOR IA</span>
        </button>
      </div>
    </header>
  );
};
