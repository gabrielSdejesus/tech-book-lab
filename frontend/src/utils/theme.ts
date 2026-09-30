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

export interface ThemeTargetElement {
  classList: {
    add: (c: string) => void;
    remove: (c: string) => void;
  };
  style?: {
    colorScheme?: string;
  };
}

export function applyTheme(
  theme: Theme,
  docElement: ThemeTargetElement = document.documentElement,
  storage: Storage = localStorage
): void {
  if (theme === 'dark') {
    docElement.classList.add('dark');
  } else {
    docElement.classList.remove('dark');
  }

  if (docElement.style) {
    docElement.style.colorScheme = theme;
  }

  try {
    storage.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    // Ignora erro de gravação se storage inacessível
  }
}
