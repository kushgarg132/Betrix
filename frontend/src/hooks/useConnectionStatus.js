import { useEffect, useReducer } from 'react';
import { wsClient } from '@/api/apolloClient';

// The browser being offline wins over socket events until it comes back.
export function connectionReducer(state, event) {
  switch (event) {
    case 'browser-offline': return 'offline';
    case 'browser-online':  return 'reconnecting';
    case 'connected':       return state === 'offline' ? state : 'online';
    case 'connecting':
    case 'closed':          return state === 'offline' ? state : 'reconnecting';
    default:                return state;
  }
}

export function useConnectionStatus(client = wsClient) {
  const [status, dispatch] = useReducer(connectionReducer, 'online');
  useEffect(() => {
    const offs = ['connecting', 'connected', 'closed'].map((e) => client.on(e, () => dispatch(e)));
    const goOffline = () => dispatch('browser-offline');
    const goOnline = () => dispatch('browser-online');
    window.addEventListener('offline', goOffline);
    window.addEventListener('online', goOnline);
    return () => {
      offs.forEach((off) => off());
      window.removeEventListener('offline', goOffline);
      window.removeEventListener('online', goOnline);
    };
  }, [client]);
  return status;
}
