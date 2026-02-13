import { api } from './api';
import { tokenManager } from '../utils/tokenManager';

export const authService = {
    login: async (username: string, password?: string) => {
        const response = await api.post('/auth/login', { username, password });
        const data = response.data.data; // { accessToken, refreshToken, user }

        return data;
    },

    logout: () => {
        tokenManager.clearTokens();
        localStorage.removeItem('admin_user');
        window.location.href = '/login';
    },

    getCurrentUser: () => {
        const userStr = localStorage.getItem('admin_user');
        return userStr ? JSON.parse(userStr) : null;
    }
};
