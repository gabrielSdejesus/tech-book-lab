export type Theme = 'dark' | 'light';

const THEME_STORAGE_KEY = 'lab_theme';

export function getInitialTheme(storage: Storage = localStorage): Theme {
  try {
    const saved = storage.getItem(THEME_STORAGE_KEY);
    if (saved === 'light' || saved === 'dark') {
      return saved;
    }
  } catch {
    // Fallback se localStorage estiver inacessível
  }
  return 'dark'; // Escuro como padrão obrigatório
}

export function toggleTheme(current: Theme): Theme {
  return current === 'dark' ? 'light' : 'dark';
}

export function applyTheme(
  theme: Theme,
  docElement: { classList: { add: (c: string) => void; remove: (c: string) => void } } = document.documentElement,
  storage: Storage = localStorage
): void {
  if (theme === 'dark') {
    docElement.classList.add('dark');
  } else {
    docElement.classList.remove('dark');
  }

  try {
    storage.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    // Ignora erro de gravação se storage inacessível
  }
}
