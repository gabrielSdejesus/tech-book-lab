export interface EngineConfig {
  type: string;
  name: string;
  port: number;
  shortLabel: string;
  connectionLabel: string;
  badgeClass: string;
  toolbarClass: string;
}

const ENGINE_REGISTRY: Record<string, EngineConfig> = {
  POSTGRES: {
    type: 'POSTGRES',
    name: 'PostgreSQL 16',
    port: 5432,
    shortLabel: 'PG',
    connectionLabel: 'PG:5432',
    badgeClass: 'bg-[#e5ebe4] dark:bg-[#1a2e1d] text-[#14532d] dark:text-[#86efac] border-[#166534]',
    toolbarClass: 'bg-[#e5ebe4] dark:bg-[#1a2e1d] text-[#14532d] dark:text-[#86efac] border-stone-800 dark:border-stone-600',
  },
  NEO4J: {
    type: 'NEO4J',
    name: 'Neo4j 5',
    port: 7687,
    shortLabel: 'NEO4J',
    connectionLabel: 'NEO4J:7687',
    badgeClass: 'bg-[#efe3d5] dark:bg-[#2d2419] text-[#713f12] dark:text-[#fde047] border-[#a16207]',
    toolbarClass: 'bg-[#efe3d5] dark:bg-[#2d2419] text-[#713f12] dark:text-[#fde047] border-stone-800 dark:border-stone-600',
  },
};

export function getEngineConfig(type?: string | null): EngineConfig {
  if (!type || !type.trim()) {
    return {
      type: 'UNKNOWN',
      name: 'Unknown Engine',
      port: 0,
      shortLabel: 'UNKNOWN',
      connectionLabel: 'UNKNOWN',
      badgeClass: 'bg-stone-100 dark:bg-stone-800 text-stone-700 dark:text-stone-300 border-stone-400 dark:border-stone-600',
      toolbarClass: 'bg-stone-100 dark:bg-stone-800 text-stone-700 dark:text-stone-300 border-stone-800 dark:border-stone-600',
    };
  }

  const normalized = type.trim().toUpperCase();
  const registered = ENGINE_REGISTRY[normalized];
  if (registered) {
    return registered;
  }

  return {
    type: normalized,
    name: normalized,
    port: 0,
    shortLabel: normalized,
    connectionLabel: normalized,
    badgeClass: 'bg-stone-100 dark:bg-stone-800 text-stone-700 dark:text-stone-300 border-stone-400 dark:border-stone-600',
    toolbarClass: 'bg-stone-100 dark:bg-stone-800 text-stone-700 dark:text-stone-300 border-stone-800 dark:border-stone-600',
  };
}

export function getConnectionLabel(config: EngineConfig, activePort?: number | null): string {
  if (activePort && activePort > 0) {
    return `${config.shortLabel}:${activePort}`;
  }
  return config.connectionLabel;
}
