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

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthProvider;