import { getSessionId } from './session';

export type LabProvisionStatus = 'NOT_PROVISIONED' | 'PROVISIONING' | 'READY' | 'STOPPING' | 'STOPPED' | 'ERROR';

export interface LabProvisionResponse {
  labId: string;
  engineType: string;
  status: LabProvisionStatus;
  message: string;
  allocatedPort: number;
  estimatedWaitSeconds: number;
}

export interface LabStatusResponse {
  labId: string;
  engineType: string;
  status: LabProvisionStatus;
  allocatedPort: number;
  uptimeSeconds: number;
  lastHeartbeatAt: number;
  errorMessage?: string | null;
}

export interface LabHeartbeatResponse {
  status: string;
  labId: string;
  ttlRemainingSeconds: number;
  lastHeartbeatAt: number;
}

export interface LabTeardownResponse {
  labId: string;
  status: LabProvisionStatus;
  message: string;
}

function getHeaders(): HeadersInit {
  return {
    'Content-Type': 'application/json',
    'X-Session-Id': getSessionId(),
  };
}

export async function provisionLab(labId: string): Promise<LabProvisionResponse> {
  const res = await fetch(`/api/lab/${encodeURIComponent(labId)}/provision`, {
    method: 'POST',
    headers: getHeaders(),
  });
  if (!res.ok) {
    throw new Error(`Falha ao provisionar laboratório: HTTP ${res.status}`);
  }
  return res.json();
}

export async function getLabStatus(labId: string): Promise<LabStatusResponse> {
  const res = await fetch(`/api/lab/${encodeURIComponent(labId)}/status`, {
    method: 'GET',
    headers: getHeaders(),
  });
  if (!res.ok) {
    throw new Error(`Falha ao consultar status do laboratório: HTTP ${res.status}`);
  }
  return res.json();
}

export async function sendHeartbeat(labId: string): Promise<LabHeartbeatResponse> {
  const res = await fetch(`/api/lab/${encodeURIComponent(labId)}/heartbeat`, {
    method: 'POST',
    headers: getHeaders(),
  });
  if (!res.ok) {
    throw new Error(`Falha ao enviar heartbeat do laboratório: HTTP ${res.status}`);
  }
  return res.json();
}

export async function teardownLab(labId: string): Promise<LabTeardownResponse> {
  const res = await fetch(`/api/lab/${encodeURIComponent(labId)}/teardown`, {
    method: 'POST',
    headers: getHeaders(),
  });
  if (!res.ok) {
    throw new Error(`Falha ao desprovisionar laboratório: HTTP ${res.status}`);
  }
  return res.json();
}
