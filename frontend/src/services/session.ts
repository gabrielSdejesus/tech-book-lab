const SESSION_STORAGE_KEY = 'tbl_session_id';
const UUID_V4_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function getSessionId(): string {
  try {
    const stored = localStorage.getItem(SESSION_STORAGE_KEY);
    if (stored && UUID_V4_REGEX.test(stored)) {
      return stored;
    }
  } catch {
    // localStorage pode estar inacessível em modo privado restrito
  }

  const newId = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : generateFallbackUuid();

  try {
    localStorage.setItem(SESSION_STORAGE_KEY, newId);
  } catch {
    // ignorar falhas de storage
  }

  return newId;
}

function generateFallbackUuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
