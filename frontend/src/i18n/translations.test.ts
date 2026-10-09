import { describe, it, expect } from 'vitest';
import { translations } from './translations';

describe('i18n Translations Parity and Integrity', () => {
  const pt = translations.pt;
  const en = translations.en;

  function assertObjectKeysAndTypesMatch(
    objA: Record<string, any>,
    objB: Record<string, any>,
    path: string[] = []
  ) {
    const keysA = Object.keys(objA).sort();
    const keysB = Object.keys(objB).sort();

    const currentPath = path.join('.');

    expect(
      keysA,
      `Chaves em '${currentPath || 'root'}' divergem entre PT e EN`
    ).toEqual(keysB);

    for (const key of keysA) {
      const valA = objA[key];
      const valB = objB[key];
      const fieldPath = [...path, key].join('.');

      expect(
        typeof valA,
        `Tipo do campo '${fieldPath}' diverge entre PT e EN`
      ).toBe(typeof valB);

      if (typeof valA === 'object' && valA !== null) {
        expect(valB).not.toBeNull();
        assertObjectKeysAndTypesMatch(valA, valB, [...path, key]);
      } else if (typeof valA === 'function') {
        expect(typeof valB).toBe('function');
      } else if (typeof valA === 'string') {
        expect(valA.trim().length, `String vazia encontrada no idioma PT para '${fieldPath}'`).toBeGreaterThan(0);
        expect(valB.trim().length, `String vazia encontrada no idioma EN para '${fieldPath}'`).toBeGreaterThan(0);
      }
    }
  }

  it('deve possuir paridade 1:1 rigorosa de chaves e tipos entre os dicionários pt e en', () => {
    assertObjectKeysAndTypesMatch(pt, en);
  });

  it('deve executar todas as funções geradoras com valores singulares e plurais em ambos os idiomas', () => {
    // bookshelf.booksAvailable
    expect(pt.bookshelf.booksAvailable(1)).toBe('1 Livro Disponível');
    expect(pt.bookshelf.booksAvailable(5)).toBe('5 Livros Disponíveis');
    expect(en.bookshelf.booksAvailable(1)).toBe('1 Available Book');
    expect(en.bookshelf.booksAvailable(5)).toBe('5 Available Books');

    // bookshelf.chaptersCount
    expect(pt.bookshelf.chaptersCount(1)).toBe('1 Capítulo Prático');
    expect(pt.bookshelf.chaptersCount(3)).toBe('3 Capítulos Práticos');
    expect(en.bookshelf.chaptersCount(1)).toBe('1 Hands-on Chapter');
    expect(en.bookshelf.chaptersCount(3)).toBe('3 Hands-on Chapters');

    // bookshelf.labsActive
    expect(pt.bookshelf.labsActive(1)).toBe('1 Laboratório Ativo');
    expect(pt.bookshelf.labsActive(4)).toBe('4 Laboratórios Ativos');
    expect(en.bookshelf.labsActive(1)).toBe('1 Active Laboratory');
    expect(en.bookshelf.labsActive(4)).toBe('4 Active Laboratories');

    // bookshelf.notebookStructure
    expect(pt.bookshelf.notebookStructure(1, 1)).toBe('Estrutura do Caderno (1 capítulo • 1 laboratório):');
    expect(pt.bookshelf.notebookStructure(2, 4)).toBe('Estrutura do Caderno (2 capítulos • 4 laboratórios):');
    expect(en.bookshelf.notebookStructure(1, 1)).toBe('Notebook Structure (1 chapter • 1 lab):');
    expect(en.bookshelf.notebookStructure(2, 4)).toBe('Notebook Structure (2 chapters • 4 labs):');

    // aiModal.activeVersion
    expect(pt.aiModal.activeVersion('gemini-1.5', 120)).toBe('Versão ativa: gemini-1.5 (120ms)');
    expect(en.aiModal.activeVersion('gemini-1.5', 120)).toBe('Active version: gemini-1.5 (120ms)');
  });

  it('deve possuir as traduções corretas para a ação de conferir gabarito reflexivo', () => {
    expect(pt.lab.checkReflectionAnswer).toBe('Conferir Gabarito de Trade-off');
    expect(en.lab.checkReflectionAnswer).toBe('Check Model Answer');
  });
});
