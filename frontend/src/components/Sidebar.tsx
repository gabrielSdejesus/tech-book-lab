import React from 'react';
import type { Book, Lab, Chapter } from '../types';
import { Bookmark, ChevronRight, PanelLeftClose } from 'lucide-react';
import { useLanguage } from '../i18n/LanguageContext';
import { formatSectionNumber } from '../utils/formatters';
import { getEngineMeta } from '../config/engines';

interface Props {
  books: Book[];
  selectedLab: Lab | null;
  onSelectLab: (lab: Lab) => void;
  isOpen?: boolean;
  onToggle?: () => void;
}

export const Sidebar: React.FC<Props> = ({
  books,
  selectedLab,
  onSelectLab,
  isOpen = true,
  onToggle,
}) => {
  const { t, locale } = useLanguage();

  return (
    <aside
      data-testid="sidebar"
      aria-label={t.sidebar.tableOfContents}
      aria-hidden={!isOpen}
      className={`bg-[#f5f2e9] dark:bg-[#181715] flex flex-col h-[calc(100vh-4.5rem)] select-none transition-all duration-300 ease-in-out shrink-0 ${
        isOpen
          ? 'w-80 border-r-2 border-stone-800 dark:border-stone-700 opacity-100 overflow-y-auto'
          : 'w-0 border-r-0 opacity-0 overflow-hidden pointer-events-none'
      }`}
    >
      {/* Book Metadata Box */}
      <div className="p-4 border-b border-stone-300 dark:border-stone-700 bg-[#efebe1] dark:bg-[#1f1d1a]">
        <div className="flex items-center justify-between mb-1.5">
          <div className="text-[10px] font-mono uppercase tracking-widest text-stone-600 dark:text-stone-400 font-bold flex items-center gap-1.5">
            <Bookmark className="w-3 h-3 text-[#8f1d1d] dark:text-[#df4444]" />
            <span>{t.sidebar.tableOfContents}</span>
          </div>
          {onToggle && (
            <button
              onClick={onToggle}
              aria-label={t.sidebar.collapseSidebar}
              title={t.sidebar.collapseSidebar}
              className="text-stone-500 hover:text-stone-900 dark:text-stone-400 dark:hover:text-stone-100 p-1 hover:bg-[#e4dfd3] dark:hover:bg-[#2e2b27] border border-stone-400 dark:border-stone-600 transition-colors"
            >
              <PanelLeftClose className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
        {books.map((book) => (
          <div key={book.id} className="space-y-1">
            <h2 className="text-sm font-serif font-bold text-stone-900 dark:text-stone-100 leading-snug">
              {book.title}
            </h2>
            <p className="text-xs font-serif italic text-stone-600 dark:text-stone-400">
              {locale === 'pt' ? 'Por' : 'By'} {book.author}
            </p>
          </div>
        ))}
      </div>

      {/* Chapters & Sections */}
      <div className="p-3 flex-1 space-y-5">
        {books.flatMap((b) => b.chapters).map((chapter: Chapter) => (
          <div key={chapter.id} className="space-y-2">
            {/* Chapter Header */}
            <div className="px-2 pt-1 pb-1 border-b border-stone-300 dark:border-stone-700">
              <span className="text-[10px] font-mono font-bold text-[#8f1d1d] dark:text-[#df4444] uppercase tracking-wider">
                {locale === 'pt' ? 'CAPÍTULO' : 'CHAPTER'} {chapter.number}
              </span>
              <h3 className="text-xs font-serif font-bold text-stone-900 dark:text-stone-100 mt-0.5">
                {chapter.title}
              </h3>
            </div>

            {/* Labs as numbered sections */}
            <div className="space-y-1.5">
              {chapter.labs.map((lab) => {
                const isSelected = selectedLab?.id === lab.id;
                return (
                  <button
                    key={lab.id}
                    onClick={() => onSelectLab(lab)}
                    className={`w-full text-left p-2.5 transition-all flex items-start gap-2.5 border text-xs ${
                      isSelected
                        ? 'bg-[#eee8db] dark:bg-[#252320] border-l-4 border-l-[#8f1d1d] dark:border-l-[#df4444] border-t-stone-400 dark:border-t-stone-700 border-r-stone-400 dark:border-r-stone-700 border-b-stone-400 dark:border-b-stone-700 text-stone-950 dark:text-stone-100 font-medium book-shadow-sm'
                        : 'border-transparent text-stone-700 dark:text-stone-300 hover:text-stone-950 dark:hover:text-stone-100 hover:bg-[#ede7da] dark:hover:bg-[#201e1b]'
                    }`}
                  >
                    <span className="mt-0.5 text-stone-500 dark:text-stone-400 font-mono text-[11px] font-bold">
                      &sect;&nbsp;{formatSectionNumber(chapter.number, lab.number)}
                    </span>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-serif font-semibold truncate text-stone-900 dark:text-stone-100">
                          {lab.title}
                        </span>
                        <ChevronRight
                          className={`w-3.5 h-3.5 shrink-0 transition-transform ${
                            isSelected ? 'text-[#8f1d1d] dark:text-[#df4444] translate-x-0.5' : 'text-stone-400 dark:text-stone-600'
                          }`}
                        />
                      </div>
                      <div className="flex items-center gap-1.5 mt-1.5">
                        <span
                          className={`px-1.5 py-0.2 text-[9px] font-mono uppercase font-bold border ${getEngineMeta(lab.engineType).badgeClass}`}
                        >
                          {lab.engineType}
                        </span>
                        <span className="text-[10px] font-mono text-stone-500 dark:text-stone-400">
                          [{lab.challenges.length}{' '}
                          {locale === 'pt'
                            ? lab.challenges.length === 1
                              ? 'desafio'
                              : 'desafios'
                            : lab.challenges.length === 1
                            ? 'challenge'
                            : 'challenges'}]
                        </span>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </aside>
  );
};
