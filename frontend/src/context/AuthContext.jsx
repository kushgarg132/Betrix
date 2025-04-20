import React, { createContext, useState, useEffect } from 'react';
import axios from '../api/axios';

export const AuthContext = createContext();

const AuthProvider = ({ children }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [user, setUser] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    setIsLoggedIn(!!token);

    if (token) {
      // Fetch user data if token exists
      axios
        .get('/user/me')
        .then((response) => setUser(response.data))
        .catch((error) => console.error('Error fetching user data:', error));
    }
  }, []);

  const login = (token) => {
    localStorage.setItem('token', token);
    setIsLoggedIn(true);

    // Fetch user data after login
    axios
      .get('/user/me')
      .then((response) => setUser(response.data))
      .catch((error) => console.error('Error fetching user data:', error));
  };

  const logout = () => {
    localStorage.removeItem('token');
    setIsLoggedIn(false);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthProvider;