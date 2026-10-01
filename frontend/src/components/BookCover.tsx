import React, { useState } from 'react';
import type { Book } from '../types';
import { BookOpen } from 'lucide-react';
import { useLanguage } from '../i18n/LanguageContext';

interface Props {
  book: Book;
  className?: string;
}

export const BookCover: React.FC<Props> = ({ book, className = '' }) => {
  const { t } = useLanguage();
  const [imageError, setImageError] = useState(false);

  // If we have an image URL and it hasn't errored out, render the classic cover image
  if (book.coverImageUrl && !imageError) {
    return (
      <div
        className={`relative overflow-hidden border-2 border-stone-800 dark:border-stone-700 bg-[#faf7ee] dark:bg-[#1a1917] book-shadow-md transition-transform duration-200 group-hover:scale-[1.02] aspect-[1/1.4] ${className}`}
      >
        <img
          src={book.coverImageUrl}
          alt={`Capa do livro ${book.title}`}
          onError={() => setImageError(true)}
          className="w-full h-full object-contain"
        />
        {/* Subtle vintage overlay vignette */}
        <div className="absolute inset-0 pointer-events-none border border-stone-900/10 shadow-inner" />
      </div>
    );
  }

  const bannerText =
    book.bannerText ||
    (book.category
      ? book.edition
        ? `${book.category} • ${book.edition}`
        : book.category
      : book.edition
      ? book.edition
      : t.bookCover.defaultBanner);

  // Graceful styled fallback honoring the classic editorial aesthetic
  return (
    <div
      className={`relative overflow-hidden border-2 border-stone-800 dark:border-stone-700 bg-[#faf7ee] dark:bg-[#201e1b] p-4 flex flex-col justify-between aspect-[1/1.4] book-shadow-md select-none group-hover:scale-[1.02] transition-transform ${className}`}
    >
      {/* Top Banner with coverColor */}
      <div
        className="w-full py-1.5 px-2 border border-stone-800 dark:border-stone-600 text-white text-center text-[9px] font-sans font-black tracking-widest uppercase mb-3 book-shadow-sm"
        style={{ backgroundColor: book.coverColor || '#059669' }}
      >
        {bannerText}
      </div>

      {/* Frame & Title */}
      <div className="border border-stone-400 dark:border-stone-700 p-3 bg-white/40 dark:bg-stone-900/40 flex-1 flex flex-col items-center justify-center text-center">
        <BookOpen className="w-8 h-8 text-stone-700 dark:text-stone-300 stroke-[1.5] mb-2" />
        <h3 className="font-serif font-black text-sm md:text-base text-stone-900 dark:text-stone-100 leading-snug">
          {book.title}
        </h3>
        <p className="text-[10px] font-serif italic text-stone-600 dark:text-stone-400 mt-1">
          {book.tagLine}
        </p>
      </div>

      {/* Author Footer */}
      <div className="mt-3 pt-2 border-t border-stone-400 dark:border-stone-700 text-center">
        <span className="text-[11px] font-serif font-bold text-stone-800 dark:text-stone-200">
          {book.author}
        </span>
      </div>
    </div>
  );
};
