import { describe, it, expect } from 'vitest';
import { formatSectionNumber } from './formatters';

describe('formatSectionNumber (TDD Seam)', () => {
  it('deve formatar número da seção combinando capítulo e laboratório', () => {
    expect(formatSectionNumber(3, 1)).toBe('3.1');
    expect(formatSectionNumber(1, 2)).toBe('1.2');
    expect(formatSectionNumber(4, 10)).toBe('4.10');
  });

  it('deve retornar apenas o número do laboratório se o capítulo não for fornecido', () => {
    expect(formatSectionNumber(undefined, 5)).toBe('5');
    expect(formatSectionNumber(null, 3)).toBe('3');
  });

  it('deve retornar apenas o número do capítulo se o laboratório não for fornecido', () => {
    expect(formatSectionNumber(2, undefined)).toBe('2');
    expect(formatSectionNumber(7, null)).toBe('7');
  });

  it('deve retornar string vazia se ambos forem nulos ou indefinidos', () => {
    expect(formatSectionNumber(undefined, undefined)).toBe('');
    expect(formatSectionNumber(null, null)).toBe('');
  });
});
