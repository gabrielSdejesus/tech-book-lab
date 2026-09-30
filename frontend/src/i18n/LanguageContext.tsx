import React, { createContext, useContext, useState } from 'react';
import type { Locale, TranslationSchema } from './types';

export interface LanguageContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: TranslationSchema;
}

const LanguageContext = createContext<LanguageContextValue | null>(null);

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [locale, setLocale] = useState<Locale>('pt');

  // Stub incompleto para o estágio Red
  const dummySchema = {} as TranslationSchema;

  return (
    <LanguageContext.Provider value={{ locale, setLocale, t: dummySchema }}>
      {children}
    </LanguageContext.Provider>
  );
};

export const useLanguage = (): LanguageContextValue => {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage deve ser utilizado dentro de um LanguageProvider');
  }
  return context;
};
