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
  const { data: meData, error: meError, refetch: refetchMe } = useQuery(GET_ME, { skip: !hasToken });

  useEffect(() => {
    if (!hasToken || (!meData && !meError)) return;
    if (meData?.me) {
      setUser(meData.me);
      localStorage.setItem('user', JSON.stringify(meData.me));
      return;
    }
    // A token the server no longer accepts (expired, signed with a rotated secret, deleted account)
    // resolves `me` to null. Drop it instead of leaving the app half signed-in.
    logout();
    toast.error('Session expired. Sign in again.', { id: 'session-expired' });
  }, [meData, meError]); // eslint-disable-line react-hooks/exhaustive-deps

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
