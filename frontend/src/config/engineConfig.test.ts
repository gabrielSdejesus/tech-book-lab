import { describe, it, expect } from 'vitest';
import { getEngineConfig } from './engineConfig';

describe('engineConfig Module', () => {
  it('deve retornar a configuração correta para POSTGRES', () => {
    const config = getEngineConfig('POSTGRES');

    expect(config.type).toBe('POSTGRES');
    expect(config.name).toBe('PostgreSQL 16');
    expect(config.port).toBe(5432);
    expect(config.connectionLabel).toBe('PG:5432');
    expect(config.badgeClass).toContain('bg-[#e5ebe4]');
    expect(config.badgeClass).toContain('border-[#166534]');
    expect(config.toolbarClass).toContain('border-stone-800');
  });

  it('deve retornar a configuração correta para NEO4J', () => {
    const config = getEngineConfig('NEO4J');

    expect(config.type).toBe('NEO4J');
    expect(config.name).toBe('Neo4j 5');
    expect(config.port).toBe(7687);
    expect(config.connectionLabel).toBe('NEO4J:7687');
    expect(config.badgeClass).toContain('bg-[#efe3d5]');
    expect(config.badgeClass).toContain('border-[#a16207]');
    expect(config.toolbarClass).toContain('border-stone-800');
  });

  it('deve normalizar o identificador em caixa baixa (ex: postgres, neo4j)', () => {
    const pgLower = getEngineConfig('postgres');
    expect(pgLower.type).toBe('POSTGRES');
    expect(pgLower.port).toBe(5432);

    const neoLower = getEngineConfig('neo4j');
    expect(neoLower.type).toBe('NEO4J');
    expect(neoLower.port).toBe(7687);
  });

  it('deve retornar fallback seguro com classes neutras para motor desconhecido (ex: REDIS)', () => {
    const config = getEngineConfig('REDIS');

    expect(config.type).toBe('REDIS');
    expect(config.name).toBe('REDIS');
    expect(config.port).toBe(0);
    expect(config.connectionLabel).toBe('REDIS');
    expect(config.badgeClass).toBeDefined();
    expect(config.toolbarClass).toBeDefined();
  });

  it('deve retornar fallback seguro para valores nulos, indefinidos ou vazios sem lançar exceção', () => {
    const nullConfig = getEngineConfig(null);
    expect(nullConfig.type).toBe('UNKNOWN');
    expect(nullConfig.name).toBe('Unknown Engine');
    expect(nullConfig.port).toBe(0);
    expect(nullConfig.connectionLabel).toBe('UNKNOWN');

    const undefinedConfig = getEngineConfig(undefined);
    expect(undefinedConfig.type).toBe('UNKNOWN');

    const emptyConfig = getEngineConfig('');
    expect(emptyConfig.type).toBe('UNKNOWN');
  });
});
