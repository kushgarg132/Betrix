const URGENT_MS = 5000;

export function turnProgress(deadlineIso, timeoutSeconds, nowMs = Date.now()) {
  if (!deadlineIso || !timeoutSeconds) return { remainingMs: null, fraction: 1, urgent: false };
  const remainingMs = Math.max(0, Date.parse(deadlineIso) - nowMs);
  const fraction = Math.min(1, remainingMs / (timeoutSeconds * 1000));
  return { remainingMs, fraction, urgent: remainingMs <= URGENT_MS };
}
