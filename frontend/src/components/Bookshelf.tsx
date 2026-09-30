import React from 'react';
import type { Book } from '../types';
import { BookCover } from './BookCover';
import { BookOpen, Layers, Terminal, Sparkles, ArrowRight } from 'lucide-react';
import { useLanguage } from '../i18n/LanguageContext';

interface Props {
  books: Book[];
  onSelectBook: (book: Book) => void;
}

export const Bookshelf: React.FC<Props> = ({ books, onSelectBook }) => {
  const { t, locale } = useLanguage();
  const totalChapters = books.reduce((acc, b) => acc + (b.chapters?.length || 0), 0);
  const totalLabs = books.reduce(
    (acc, b) => acc + (b.chapters?.reduce((cAcc, c) => cAcc + (c.labs?.length || 0), 0) || 0),
    0
  );

  return (
    <main className="flex-1 bg-[#fbf9f4] dark:bg-[#141312] text-stone-900 dark:text-stone-100 overflow-y-auto px-6 py-8 md:py-12 transition-colors">
      <div className="max-w-6xl mx-auto space-y-10">
        {/* Editorial Masthead / Hero */}
        <section className="border-b-2 border-stone-800 dark:border-stone-700 pb-8 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="space-y-1.5">
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono uppercase px-2 py-0.5 border border-stone-700 dark:border-stone-600 bg-[#efebe1] dark:bg-[#201e1b] text-[#8f1d1d] dark:text-[#df4444] font-bold tracking-widest">
                  {t.bookshelf.catalogTag}
                </span>
                <span className="text-xs font-mono text-stone-500 dark:text-stone-400">
                  {t.bookshelf.version}
                </span>
              </div>
              <h1 className="text-2xl md:text-4xl font-serif font-black tracking-tight text-stone-900 dark:text-stone-100 uppercase">
                {t.bookshelf.title}
              </h1>
              <p className="text-sm md:text-base font-serif italic text-stone-600 dark:text-stone-400 max-w-3xl leading-relaxed">
                {t.bookshelf.subtitle}
              </p>
            </div>

            {/* Platform Highlights / Stats */}
            <div className="flex flex-wrap gap-2.5 font-mono text-xs">
              <div className="bg-[#f7f4ec] dark:bg-[#1b1917] border-2 border-stone-800 dark:border-stone-700 px-3 py-2 book-shadow-sm flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-[#8f1d1d] dark:text-[#df4444]" />
                <span className="font-bold text-stone-900 dark:text-stone-100">
                  {t.bookshelf.booksAvailable(books.length)}
                </span>
              </div>
              <div className="bg-[#f7f4ec] dark:bg-[#1b1917] border-2 border-stone-800 dark:border-stone-700 px-3 py-2 book-shadow-sm flex items-center gap-2">
                <Layers className="w-4 h-4 text-[#059669]" />
                <span className="font-bold text-stone-900 dark:text-stone-100">
                  {t.bookshelf.chaptersCount(totalChapters)}
                </span>
              </div>
              <div className="bg-[#f7f4ec] dark:bg-[#1b1917] border-2 border-stone-800 dark:border-stone-700 px-3 py-2 book-shadow-sm flex items-center gap-2">
                <Terminal className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                <span className="font-bold text-stone-900 dark:text-stone-100">
                  {t.bookshelf.labsActive(totalLabs)}
                </span>
              </div>
            </div>
          </div>
        </section>


        {/* Bookshelf Catalog Grid */}
        <section className="space-y-6">
          <div className="flex items-center justify-between border-b border-stone-300 dark:border-stone-800 pb-2">

            <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-stone-700 dark:text-stone-300 flex items-center gap-2">
              <Sparkles className="w-3.5 h-3.5 text-[#8f1d1d] dark:text-[#df4444]" />
              {t.bookshelf.classicCollection}
            </h2>
            <span className="text-[11px] font-serif italic text-stone-500 dark:text-stone-400">
              {t.bookshelf.clickToEnter}
            </span>
          </div>

          <div className="grid grid-cols-1 gap-8">
            {books.map((book) => {
              const allLabs = book.chapters?.flatMap((c) => c.labs) || [];
              const engineTypes = Array.from(new Set(allLabs.map((l) => l.engineType)));

              return (
                <article
                  key={book.id}
                  role="button"
                  tabIndex={0}
                  aria-label={book.title}
                  onClick={() => onSelectBook(book)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      onSelectBook(book);
                    }
                  }}
                  className="group cursor-pointer bg-[#f7f4ec] dark:bg-[#1a1917] border-2 border-stone-800 dark:border-stone-700 p-6 md:p-8 book-shadow-md hover:book-shadow-lg hover:border-stone-900 dark:hover:border-stone-500 focus-visible:ring-2 focus-visible:ring-[#8f1d1d] focus-visible:outline-none dark:focus-visible:ring-[#df4444] transition-all flex flex-col md:flex-row gap-6 md:gap-8 items-start select-none"
                >
                  {/* Book Cover Visual (Fixed Aspect Classic Ratio) */}
                  <div className="w-48 sm:w-56 md:w-64 shrink-0 mx-auto md:mx-0">
                    <BookCover book={book} />
                  </div>

                  {/* Book Metadata & Chapters Summary */}
                  <div className="flex-1 flex flex-col justify-between h-full space-y-5">
                    <div className="space-y-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className="px-2 py-0.5 text-[10px] font-mono font-black uppercase text-white border border-stone-800 dark:border-stone-600 book-shadow-sm"
                          style={{ backgroundColor: book.coverColor || '#059669' }}
                        >
                          {locale === 'pt' ? 'OBRA CLÁSSICA' : 'CLASSIC WORK'}
                        </span>
                        <div className="flex items-center gap-1.5">
                          {engineTypes.map((engine) => (
                            <span
                              key={engine}
                              className={`px-1.5 py-0.2 text-[9px] font-mono uppercase font-bold border ${
                                engine === 'NEO4J'
                                  ? 'bg-[#efe3d5] dark:bg-[#2d2419] text-[#713f12] dark:text-[#fde047] border-[#a16207]'
                                  : 'bg-[#e5ebe4] dark:bg-[#1a2e1d] text-[#14532d] dark:text-[#86efac] border-[#166534]'
                              }`}
                            >
                              {engine}
                            </span>
                          ))}
                        </div>
                      </div>

                      <div>
                        <h3 className="text-xl md:text-2xl font-serif font-black text-stone-900 dark:text-stone-100 group-hover:text-[#8f1d1d] dark:group-hover:text-[#df4444] transition-colors leading-snug">
                          {book.title}
                        </h3>
                        <p className="text-xs font-serif italic text-stone-600 dark:text-stone-400 mt-0.5">
                          {locale === 'pt' ? 'Por' : 'By'}{' '}
                          <strong className="font-semibold text-stone-800 dark:text-stone-200">{book.author}</strong>
                        </p>
                      </div>

                      <p className="text-xs md:text-sm font-serif font-medium text-stone-800 dark:text-stone-200 leading-relaxed border-l-2 border-[#8f1d1d] dark:border-[#df4444] pl-3 py-0.5">
                        {book.tagLine}
                      </p>

                      <p className="text-xs text-stone-600 dark:text-stone-400 font-serif leading-relaxed line-clamp-3">
                        {book.description}
                      </p>
                    </div>

                    {/* Chapters preview */}
                    <div className="pt-4 border-t border-stone-300 dark:border-stone-800 space-y-3">
                      <div className="text-[11px] font-mono font-bold uppercase tracking-wider text-stone-500 dark:text-stone-400">
                        {t.bookshelf.notebookStructure(book.chapters?.length || 0, allLabs.length)}
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        {book.chapters?.map((ch) => (
                          <div
                            key={ch.id}
                            className="bg-[#efebe1] dark:bg-[#23211e] border border-stone-300 dark:border-stone-700 px-3 py-2 text-xs font-serif space-y-0.5"
                          >
                            <span className="text-[10px] font-mono font-bold text-[#8f1d1d] dark:text-[#df4444]">
                              {locale === 'pt' ? 'CAPÍTULO' : 'CHAPTER'} {ch.number}
                            </span>
                            <div className="font-semibold truncate text-stone-900 dark:text-stone-100">
                              {ch.title}
                            </div>
                            <div className="text-[10px] font-mono text-stone-500 dark:text-stone-400">
                              {ch.labs.length}{' '}
                              {locale === 'pt'
                                ? ch.labs.length === 1
                                  ? 'exercício prático'
                                  : 'exercícios práticos'
                                : ch.labs.length === 1
                                ? 'hands-on lab'
                                : 'hands-on labs'}
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Action Button */}
                      <div className="pt-2 flex items-center justify-end">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            onSelectBook(book);
                          }}
                          className="px-5 py-2.5 bg-[#8f1d1d] hover:bg-[#771818] border-2 border-stone-900 dark:border-stone-600 text-white text-xs font-mono font-bold uppercase tracking-wide flex items-center gap-2 book-shadow-sm book-shadow-pressed transition-all"
                        >
                          <span>{t.bookshelf.openNotebook}</span>
                          <ArrowRight className="w-4 h-4 stroke-[2.2]" />
                        </button>
                      </div>
                    </div>
                  </div>
                </article>
              );
            })}

            {books.length === 0 && (
              <div className="p-12 text-center border-2 border-dashed border-stone-400 dark:border-stone-700 bg-[#efebe1] dark:bg-[#1a1917]">
                <p className="font-serif italic text-stone-600 dark:text-stone-400 text-sm">
                  {t.bookshelf.emptyCatalog}
                </p>
              </div>
            )}
          </div>
        </section>
      </div>
    </main>

  );
};
