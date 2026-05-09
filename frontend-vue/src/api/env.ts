export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
export const APP_MODE = import.meta.env.VITE_APP_MODE || 'mock';
export const USE_BACKEND = APP_MODE === 'backend' || APP_MODE === 'redis';
