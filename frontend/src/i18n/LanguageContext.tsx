import React, { createContext, useContext, useState, useEffect } from 'react';
import type { Locale, TranslationSchema } from './types';
import { translations } from './translations';

export interface LanguageContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: TranslationSchema;
}

const STORAGE_KEY = 'tbl_locale';

const LanguageContext = createContext<LanguageContextValue | null>(null);

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [locale, setLocaleState] = useState<Locale>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved === 'en' || saved === 'pt') {
        return saved;
      }
    } catch {
      // Ignora erro de acesso a localStorage em ambientes restritos
    }
    return 'pt';
  });

  const setLocale = (newLocale: Locale) => {
    setLocaleState(newLocale);
    try {
      localStorage.setItem(STORAGE_KEY, newLocale);
    } catch {
      // Ignora erro
    }
  };

  useEffect(() => {
    document.documentElement.lang = locale === 'pt' ? 'pt-BR' : 'en-US';
  }, [locale]);

  const value: LanguageContextValue = {
    locale,
    setLocale,
    t: translations[locale],
  };

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useLanguage = (): LanguageContextValue => {
  const context = useContext(LanguageContext);
  if (!context) {
    return {
      locale: 'pt',
      setLocale: () => {},
      t: translations.pt,
    };
  }
  return context;
};

