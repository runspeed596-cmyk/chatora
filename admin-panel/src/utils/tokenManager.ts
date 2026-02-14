const STORAGE_KEY_ACCESS = 'access_token';
const STORAGE_KEY_REFRESH = 'refresh_token';

export const tokenManager = {
    getAccessToken: (): string | null => {
        return localStorage.getItem(STORAGE_KEY_ACCESS);
    },
    getRefreshToken: (): string | null => {
        return localStorage.getItem(STORAGE_KEY_REFRESH);
    },
    setAccessToken: (token: string) => {
        localStorage.setItem(STORAGE_KEY_ACCESS, token);
    },
    setRefreshToken: (token: string) => {
        localStorage.setItem(STORAGE_KEY_REFRESH, token);
    },
    clearTokens: () => {
        localStorage.removeItem(STORAGE_KEY_ACCESS);
        localStorage.removeItem(STORAGE_KEY_REFRESH);
        localStorage.removeItem('admin_user');
    }
};
