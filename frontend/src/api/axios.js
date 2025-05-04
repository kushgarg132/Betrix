import axios from 'axios';
import API_CONFIG from '../config/apiConfig';

const axiosInstance = axios.create({
  baseURL: API_CONFIG.API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add a request interceptor (optional)
axiosInstance.interceptors.request.use(
  (config) => {
    // Add authorization token if available
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Add a response interceptor (optional)
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle errors globally
    console.error('API Error:', error.response || error.message);
    return Promise.reject(error);
  }
);

export default axiosInstance;