import React, { createContext, useState, useEffect } from 'react';
import { useQuery } from '@apollo/client/react';
import { GET_ME } from '../graphql/queries';

export const AuthContext = createContext();

const AuthProvider = ({ children }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(() => !!localStorage.getItem('token'));
  const [user, setUser] = useState(() => {
    const storedUser = localStorage.getItem('user');
    return storedUser ? JSON.parse(storedUser) : null;
  });

  const hasToken = !!localStorage.getItem('token');

  const { data: meData, refetch: refetchMe } = useQuery(GET_ME, {
    skip: !hasToken,
    onCompleted: (data) => {
      if (data?.me) {
        setUser(data.me);
        localStorage.setItem('user', JSON.stringify(data.me));
      }
    },
    onError: (error) => {
      console.error('Error fetching user data:', error);
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      setUser(null);
      setIsLoggedIn(false);
    },
  });

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

  const updateBalance = (newBalance) => {
    if (user) {
      const updatedUser = { ...user, balance: newBalance };
      setUser(updatedUser);
      localStorage.setItem('user', JSON.stringify(updatedUser));
    }
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
      updateBalance,
      refreshUserData
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthProvider;
