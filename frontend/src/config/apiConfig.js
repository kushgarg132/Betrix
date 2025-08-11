// API Configuration
const API_CONFIG = {
  // Server host and port - fetched from Firebase environment variables with fallbacks
  get SERVER_HOST() {
    return process.env.REACT_APP_SERVER_HOST || 'http://localhost';
  },
  get SERVER_PORT() {
    // Use nullish coalescing so empty string ('') is respected
    return process.env.REACT_APP_SERVER_PORT ?? '8080';
  },
  
  // Build base host, omitting the port if it's an empty string
  buildBaseHost() {
    return this.SERVER_PORT === '' ? this.SERVER_HOST : `${this.SERVER_HOST}:${this.SERVER_PORT}`;
  },
  
  // WebSocket URL (without /ws path)
  get WS_URL() {
    return `${this.buildBaseHost()}/ws`;
  },
  
  // API base URL
  get API_BASE_URL() {
    return `${this.buildBaseHost()}/api`;
  }
};

export default API_CONFIG; 