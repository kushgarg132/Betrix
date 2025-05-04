// API Configuration
const API_CONFIG = {
  // Server host and port - fetched from Firebase environment variables with fallbacks
  get SERVER_HOST() {
    return process.env.REACT_APP_SERVER_HOST || 'https://betrix-backend.onrender.com';
  },
  get SERVER_PORT() {
    return process.env.REACT_APP_SERVER_PORT || '';
  },
  
  // WebSocket URL (without /ws path)
  get WS_URL() {
    return `${this.SERVER_HOST}:${this.SERVER_PORT}/ws`;
  },
  
  // API base URL
  get API_BASE_URL() {
    return `${this.SERVER_HOST}:${this.SERVER_PORT}/api`;
  }
};

export default API_CONFIG; 