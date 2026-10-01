export interface EngineMeta {
  name: string;
  badgeClass: string;
  queryLanguage: string;
  shortLabel: string;
  defaultPort: number;
}

export const ENGINE_CONFIGS: Record<string, EngineMeta> = {
  POSTGRES: {
    name: 'PostgreSQL 16',
    badgeClass: 'bg-[#e5ebe4] dark:bg-[#1a2e1d] text-[#14532d] dark:text-[#86efac] border-[#166534] dark:border-[#15803d]',
    queryLanguage: 'SQL',
    shortLabel: 'PG',
    defaultPort: 5432,
  },
  NEO4J: {
    name: 'Neo4j 5',
    badgeClass: 'bg-[#efe3d5] dark:bg-[#2d2419] text-[#713f12] dark:text-[#fde047] border-[#a16207] dark:border-[#854d0e]',
    queryLanguage: 'CYPHER',
    shortLabel: 'NEO4J',
    defaultPort: 7687,
  },
};

export function getEngineMeta(engineType?: string): EngineMeta {
  const key = engineType ? engineType.toUpperCase() : 'POSTGRES';
  return (
    ENGINE_CONFIGS[key] || {
      name: key,
      badgeClass: 'bg-stone-200 dark:bg-stone-800 text-stone-800 dark:text-stone-200 border-stone-400 dark:border-stone-600',
      queryLanguage: 'QUERY',
      shortLabel: key,
      defaultPort: 0,
    }
  );
}

export function formatEngineLabel(meta: EngineMeta, locale: 'pt' | 'en' = 'pt'): string {
  const prefix = locale === 'pt' ? 'MOTOR' : 'ENGINE';
  return `${prefix}: ${meta.name.toUpperCase()} (${meta.queryLanguage})`;
}
