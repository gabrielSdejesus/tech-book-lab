import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import React from 'react';
import { LanguageProvider, useLanguage } from './LanguageContext';

describe('LanguageContext', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <LanguageProvider>{children}</LanguageProvider>
  );

  it('deve inicializar com o idioma "pt" por padrão e persistir em localStorage', () => {
    const { result } = renderHook(() => useLanguage(), { wrapper });

    expect(result.current.locale).toBe('pt');
    expect(result.current.t.bookshelf.title).toBe('Biblioteca de Livros Técnicos');
    expect(result.current.t.common.bookshelf).toBe('Estante de Livros');
  });

  it('deve alternar para "en", atualizar os textos e salvar no localStorage', () => {
    const { result } = renderHook(() => useLanguage(), { wrapper });

    act(() => {
      result.current.setLocale('en');
    });

    expect(result.current.locale).toBe('en');
    expect(localStorage.getItem('tbl_locale')).toBe('en');
    expect(result.current.t.bookshelf.title).toBe('Technical Books Library');
    expect(result.current.t.common.bookshelf).toBe('Bookshelf');
  });

  it('deve restaurar o idioma a partir do localStorage caso já exista', () => {
    localStorage.setItem('tbl_locale', 'en');

    const { result } = renderHook(() => useLanguage(), { wrapper });

    expect(result.current.locale).toBe('en');
    expect(result.current.t.common.bookshelf).toBe('Bookshelf');
  });
});
