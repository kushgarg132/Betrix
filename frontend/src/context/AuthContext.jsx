import React, { createContext, useState, useEffect } from 'react';
import { useQuery } from '@apollo/client/react';
import { toast } from 'sonner';
import { GET_ME } from '../graphql/queries';

export const AuthContext = createContext();

const AuthProvider = ({ children }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(() => !!localStorage.getItem('token'));
  const [user, setUser] = useState(() => {
    const storedUser = localStorage.getItem('user');
    return storedUser ? JSON.parse(storedUser) : null;
  });

  const hasToken = !!localStorage.getItem('token');
  const { data: meData, refetch: refetchMe } = useQuery(GET_ME, { skip: !hasToken });

  useEffect(() => {
    // Only an authoritative `me: null` means the token is really invalid (expired, rotated
    // secret, deleted account) - log out for that. A query error (network blip, backend
    // restart, 5xx) says nothing about the token's validity, so it must NOT clear a perfectly
    // good session; leave the token and user alone and let the next query attempt retry.
    if (!hasToken || !meData) return;
    if (meData.me) {
      setUser(meData.me);
      localStorage.setItem('user', JSON.stringify(meData.me));
      return;
    }
    logout();
    toast.error('Session expired. Sign in again.', { id: 'session-expired' });
  }, [meData]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const token = localStorage.getItem('token');
    setIsLoggedIn(!!token);
  }, []);

  const login = async (token) => {
    localStorage.setItem('token', token);
    setIsLoggedIn(true);

    try {
      const { data } = await refetchMe();
      if (data?.me) {
        setUser(data.me);
        localStorage.setItem('user', JSON.stringify(data.me));
      }
    } catch (error) {
      console.error('Error fetching user data:', error);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setIsLoggedIn(false);
    setUser(null);
  };

  const refreshUserData = async () => {
    try {
      const { data } = await refetchMe();
      if (data?.me) {
        setUser(data.me);
        localStorage.setItem('user', JSON.stringify(data.me));
        return data.me;
      }
    } catch (error) {
      console.error('Error refreshing user data:', error);
    }
    return null;
  };

  return (
    <AuthContext.Provider value={{
      isLoggedIn,
      user,
      setUser,
      login,
      logout,
      refreshUserData
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthProvider;
