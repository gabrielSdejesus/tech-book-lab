import { describe, it, expect, vi, beforeEach } from 'vitest';
import { provisionLab, getLabStatus, sendHeartbeat, teardownLab } from './labProvisioning';

describe('labProvisioning API client', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('tbl_session_id', '12345678-1234-4234-8234-123456789abc');
    vi.restoreAllMocks();
  });

  it('deve chamar POST /api/lab/:labId/provision com header X-Session-Id', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        labId: 'ddia-cap-03-lab-01',
        engineType: 'POSTGRES',
        status: 'READY',
        allocatedPort: 5432,
      }),
    } as any);

    const result = await provisionLab('ddia-cap-03-lab-01');

    expect(global.fetch).toHaveBeenCalledWith('/api/lab/ddia-cap-03-lab-01/provision', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Session-Id': '12345678-1234-4234-8234-123456789abc',
      },
    });
    expect(result.status).toBe('READY');
  });

  it('deve chamar GET /api/lab/:labId/status com header X-Session-Id', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        labId: 'ddia-cap-03-lab-01',
        engineType: 'POSTGRES',
        status: 'READY',
        allocatedPort: 5432,
      }),
    } as any);

    const result = await getLabStatus('ddia-cap-03-lab-01');

    expect(global.fetch).toHaveBeenCalledWith('/api/lab/ddia-cap-03-lab-01/status', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'X-Session-Id': '12345678-1234-4234-8234-123456789abc',
      },
    });
    expect(result.status).toBe('READY');
  });

  it('deve chamar POST /api/lab/:labId/heartbeat com header X-Session-Id', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        status: 'ACK',
        ttlRemainingSeconds: 900,
      }),
    } as any);

    const result = await sendHeartbeat('ddia-cap-03-lab-01');

    expect(global.fetch).toHaveBeenCalledWith('/api/lab/ddia-cap-03-lab-01/heartbeat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Session-Id': '12345678-1234-4234-8234-123456789abc',
      },
    });
    expect(result.status).toBe('ACK');
  });

  it('deve chamar POST /api/lab/:labId/teardown com header X-Session-Id', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        labId: 'ddia-cap-03-lab-01',
        status: 'STOPPED',
      }),
    } as any);

    const result = await teardownLab('ddia-cap-03-lab-01');

    expect(global.fetch).toHaveBeenCalledWith('/api/lab/ddia-cap-03-lab-01/teardown', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Session-Id': '12345678-1234-4234-8234-123456789abc',
      },
    });
    expect(result.status).toBe('STOPPED');
  });

  it('deve incluir challengeId como query parameter ao provisionar tarefa específica', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        labId: 'ddia-cap-03-lab-02',
        challengeId: 'lab-02-ch-2',
        engineType: 'POSTGRES',
        status: 'READY',
        allocatedPort: 5432,
      }),
    } as any);

    const result = await provisionLab('ddia-cap-03-lab-02', 'lab-02-ch-2');

    expect(global.fetch).toHaveBeenCalledWith('/api/lab/ddia-cap-03-lab-02/provision?challengeId=lab-02-ch-2', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Session-Id': '12345678-1234-4234-8234-123456789abc',
      },
    });
    expect(result.challengeId).toBe('lab-02-ch-2');
    expect(result.engineType).toBe('POSTGRES');
  });

  it('deve incluir challengeId como query parameter ao consultar status da tarefa', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        labId: 'ddia-cap-03-lab-02',
        challengeId: 'lab-02-ch-2',
        engineType: 'POSTGRES',
        status: 'READY',
        allocatedPort: 5432,
      }),
    } as any);

    const result = await getLabStatus('ddia-cap-03-lab-02', 'lab-02-ch-2');

    expect(global.fetch).toHaveBeenCalledWith('/api/lab/ddia-cap-03-lab-02/status?challengeId=lab-02-ch-2', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'X-Session-Id': '12345678-1234-4234-8234-123456789abc',
      },
    });
    expect(result.challengeId).toBe('lab-02-ch-2');
  });
});
