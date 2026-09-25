import { act, render, renderHook, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { connectionReducer, useConnectionStatus } from '../useConnectionStatus';
import ConnectionPill from '@/components/poker/ConnectionPill';

function fakeClient() {
  const listeners = {};
  return {
    on: (event, cb) => { (listeners[event] ||= []).push(cb); return () => {}; },
    emit: (event) => (listeners[event] || []).forEach((cb) => cb()),
  };
}

describe('connectionReducer', () => {
  it('maps socket events to a status', () => {
    expect(connectionReducer('online', 'connecting')).toBe('reconnecting');
    expect(connectionReducer('reconnecting', 'connected')).toBe('online');
    expect(connectionReducer('online', 'closed')).toBe('reconnecting');
    expect(connectionReducer('reconnecting', 'browser-offline')).toBe('offline');
    expect(connectionReducer('offline', 'browser-online')).toBe('reconnecting');
    expect(connectionReducer('offline', 'closed')).toBe('offline');
  });
});

describe('useConnectionStatus', () => {
  it('follows the client events', () => {
    const client = fakeClient();
    const { result } = renderHook(() => useConnectionStatus(client));
    expect(result.current).toBe('online');
    act(() => client.emit('closed'));
    expect(result.current).toBe('reconnecting');
    act(() => client.emit('connected'));
    expect(result.current).toBe('online');
  });
});

describe('ConnectionPill', () => {
  it('is hidden when online and announces trouble otherwise', () => {
    const { container, rerender } = render(<ConnectionPill status="online" />);
    expect(container).toBeEmptyDOMElement();
    rerender(<ConnectionPill status="offline" />);
    expect(screen.getByRole('status')).toHaveTextContent(/offline/i);
  });
});
