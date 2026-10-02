import { describe, it, expect, beforeEach, vi } from 'vitest';
import { getSessionId } from './session';

describe('session service', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  const uuidV4Regex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

  it('deve gerar e salvar um novo sessionId no formato UUID v4 quando localStorage estiver vazio', () => {
    const id = getSessionId();

    expect(id).toMatch(uuidV4Regex);
    expect(localStorage.getItem('tbl_session_id')).toBe(id);
  });

  it('deve reutilizar o sessionId existente quando for um UUID v4 válido', () => {
    const existing = '12345678-1234-4234-8234-123456789abc';
    localStorage.setItem('tbl_session_id', existing);

    const id = getSessionId();
    expect(id).toBe(existing);
  });

  it('deve sanitizar e substituir sessionId se estiver corrompido ou malicioso', () => {
    localStorage.setItem('tbl_session_id', '../../etc/passwd');

    const id = getSessionId();
    expect(id).toMatch(uuidV4Regex);
    expect(id).not.toBe('../../etc/passwd');
    expect(localStorage.getItem('tbl_session_id')).toBe(id);
  });

  it('deve utilizar o gerador fallback quando crypto.randomUUID não estiver disponível', () => {
    const originalRandomUUID = crypto.randomUUID;
    try {
      delete (crypto as any).randomUUID;
      const id = getSessionId();
      expect(id).toMatch(uuidV4Regex);
      expect(localStorage.getItem('tbl_session_id')).toBe(id);
    } finally {
      (crypto as any).randomUUID = originalRandomUUID;
    }
  });

  it('deve ser resiliente quando localStorage.getItem lançar exceção', () => {
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem').mockImplementationOnce(() => {
      throw new Error('SecurityError: Access is denied');
    });

    const id = getSessionId();
    expect(id).toMatch(uuidV4Regex);
    getItemSpy.mockRestore();
  });

  it('deve ser resiliente quando localStorage.setItem lançar exceção', () => {
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementationOnce(() => {
      throw new Error('QuotaExceededError');
    });

    const id = getSessionId();
    expect(id).toMatch(uuidV4Regex);
    setItemSpy.mockRestore();
  });
});
