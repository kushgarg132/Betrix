import React, { createContext, useState, useEffect } from 'react';
import axios from '../api/axios';

export const AuthContext = createContext();

const AuthProvider = ({ children }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [user, setUser] = useState(() => {
    const storedUser = localStorage.getItem('user');
    return storedUser ? JSON.parse(storedUser) : null;
  });

  useEffect(() => {
    const token = localStorage.getItem('token');
    setIsLoggedIn(!!token);

    if (token) {
      // Fetch user data if token exists and user is not already set
      axios
        .get('/user/me')
        .then((response) => {
          setUser(response.data);
          localStorage.setItem('user', JSON.stringify(response.data)); // Persist user data
        })
        .catch((error) => {
          console.error('Error fetching user data:', error);
          localStorage.removeItem('token'); // Remove invalid token
          localStorage.removeItem('user');
          setUser(null); // Clear persisted user data
          setIsLoggedIn(false);
        });
    }
  }, []);

  const login = (token) => {
    localStorage.setItem('token', token);
    setIsLoggedIn(true);

    // Fetch user data after login
    axios
      .get('/user/me')
      .then((response) => {
        setUser(response.data);
        localStorage.setItem('user', JSON.stringify(response.data)); // Persist user data
      })
      .catch((error) => console.error('Error fetching user data:', error));
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user'); // Clear persisted user data
    setIsLoggedIn(false);
    setUser(null);
  };

  // Function to update user balance
  const updateBalance = (newBalance) => {
    if (user) {
      const updatedUser = { ...user, balance: newBalance };
      setUser(updatedUser);
      localStorage.setItem('user', JSON.stringify(updatedUser));
    }
  };

  // Function to refresh user data from the server
  const refreshUserData = async () => {
    try {
      const response = await axios.get('/user/me');
      setUser(response.data);
      localStorage.setItem('user', JSON.stringify(response.data));
      return response.data;
    } catch (error) {
      console.error('Error refreshing user data:', error);
      return null;
    }
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