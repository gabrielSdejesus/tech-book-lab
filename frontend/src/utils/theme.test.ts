import { describe, it, expect, beforeEach } from 'vitest';
import { getInitialTheme, toggleTheme, applyTheme } from './theme';

describe('Theme Management (TDD Seam)', () => {
  let mockStorage: Record<string, string>;

  beforeEach(() => {
    mockStorage = {};
  });

  const fakeStorage: Storage = {
    getItem: (key: string) => mockStorage[key] ?? null,
    setItem: (key: string, value: string) => {
      mockStorage[key] = value;
    },
    removeItem: (key: string) => {
      delete mockStorage[key];
    },
    clear: () => {
      mockStorage = {};
    },
    key: () => null,
    length: 0
  };

  it('deve retornar "dark" como tema padrão caso não haja preferência salva', () => {
    const theme = getInitialTheme(fakeStorage);
    expect(theme).toBe('dark');
  });

  it('deve respeitar a preferência "light" salva no storage', () => {
    fakeStorage.setItem('lab_theme', 'light');
    const theme = getInitialTheme(fakeStorage);
    expect(theme).toBe('light');
  });

  it('deve alternar corretamente entre "dark" e "light"', () => {
    expect(toggleTheme('dark')).toBe('light');
    expect(toggleTheme('light')).toBe('dark');
  });

  it('deve aplicar a classe "dark" e salvar no storage quando o tema for escuro', () => {
    const classes = new Set<string>();
    const fakeElement = {
      classList: {
        add: (c: string) => classes.add(c),
        remove: (c: string) => classes.delete(c)
      }
    };

    applyTheme('dark', fakeElement as any, fakeStorage);

    expect(classes.has('dark')).toBe(true);
    expect(fakeStorage.getItem('lab_theme')).toBe('dark');
  });

  it('deve remover a classe "dark" e salvar no storage quando o tema for claro', () => {
    const classes = new Set<string>(['dark']);
    const fakeElement = {
      classList: {
        add: (c: string) => classes.add(c),
        remove: (c: string) => classes.delete(c)
      }
    };

    applyTheme('light', fakeElement as any, fakeStorage);

    expect(classes.has('dark')).toBe(false);
    expect(fakeStorage.getItem('lab_theme')).toBe('light');
  });

  it('deve definir style.colorScheme correspondente ao tema aplicado', () => {
    const fakeElement = {
      classList: {
        add: () => {},
        remove: () => {}
      },
      style: {
        colorScheme: ''
      }
    };

    applyTheme('light', fakeElement as any, fakeStorage);
    expect(fakeElement.style.colorScheme).toBe('light');

    applyTheme('dark', fakeElement as any, fakeStorage);
    expect(fakeElement.style.colorScheme).toBe('dark');
  });

  it('deve possuir a diretiva @custom-variant dark no index.css para suportar alternância de tema no Tailwind v4', async () => {
    const fs = await import('node:fs');
    const path = await import('node:path');
    const cssPath = path.resolve(process.cwd(), 'src/index.css');
    const css = fs.readFileSync(cssPath, 'utf-8');

    expect(css).toMatch(/@custom-variant\s+dark\s+\(&:where\(\.dark,\s*\.dark\s*\*\)\);/);
  });
});

