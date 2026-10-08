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
        containerName: 'tbl-lab-postgres',
        engineType: 'POSTGRES',
        status: 'READY',
        allocatedPort: 5432,
        heartbeatIntervalSeconds: 30,
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
    expect(result.containerName).toBe('tbl-lab-postgres');
    expect(result.heartbeatIntervalSeconds).toBe(30);
  });

  it('deve chamar GET /api/lab/:labId/status com header X-Session-Id', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        labId: 'ddia-cap-03-lab-01',
        containerName: 'tbl-lab-postgres',
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
    expect(result.containerName).toBe('tbl-lab-postgres');
  });

  it('deve chamar POST /api/lab/:labId/heartbeat com header X-Session-Id e retornar TTL de 900s (15 minutos)', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        status: 'ACK',
        labId: 'ddia-cap-03-lab-01',
        ttlRemainingSeconds: 900,
        lastHeartbeatAt: Date.now(),
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
    expect(result.ttlRemainingSeconds).toBe(900);
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

  it('deve lançar erro quando provisionLab retornar resposta HTTP com status de erro', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
    } as any);

    await expect(provisionLab('ddia-cap-03-lab-01')).rejects.toThrow('Falha ao provisionar laboratório: HTTP 500');
  });

  it('deve lançar erro quando getLabStatus retornar resposta HTTP com status de erro', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
    } as any);

    await expect(getLabStatus('ddia-cap-03-lab-01')).rejects.toThrow('Falha ao consultar status do laboratório: HTTP 404');
  });

  it('deve lançar erro quando sendHeartbeat retornar resposta HTTP com status de erro', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 410,
    } as any);

    await expect(sendHeartbeat('ddia-cap-03-lab-01')).rejects.toThrow('Falha ao enviar heartbeat do laboratório: HTTP 410');
  });

  it('deve lançar erro quando teardownLab retornar resposta HTTP com status de erro', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
    } as any);

    await expect(teardownLab('ddia-cap-03-lab-01')).rejects.toThrow('Falha ao desprovisionar laboratório: HTTP 500');
  });

  it('deve propagar erro de rede quando fetch falhar em qualquer operação de provisionamento', async () => {
    global.fetch = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));

    await expect(provisionLab('ddia-cap-03-lab-01')).rejects.toThrow('Failed to fetch');
    await expect(getLabStatus('ddia-cap-03-lab-01')).rejects.toThrow('Failed to fetch');
    await expect(sendHeartbeat('ddia-cap-03-lab-01')).rejects.toThrow('Failed to fetch');
    await expect(teardownLab('ddia-cap-03-lab-01')).rejects.toThrow('Failed to fetch');
  });

  it('deve lançar erro com status 410 quando sessão expirar no provisionLab ou getLabStatus', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 410,
    } as any);

    await expect(provisionLab('ddia-cap-03-lab-01')).rejects.toThrow('Falha ao provisionar laboratório: HTTP 410');
    await expect(getLabStatus('ddia-cap-03-lab-01')).rejects.toThrow('Falha ao consultar status do laboratório: HTTP 410');
  });
});
