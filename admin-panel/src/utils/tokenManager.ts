let accessToken: string | null = localStorage.getItem('access_token');
let refreshToken: string | null = localStorage.getItem('refresh_token');

export const tokenManager = {
    getAccessToken: () => accessToken,
    getRefreshToken: () => refreshToken,
    setAccessToken: (token: string, persist = false) => {
        accessToken = token;
        if (persist) localStorage.setItem('access_token', token);
        else localStorage.removeItem('access_token');
    },
    setRefreshToken: (token: string, persist = false) => {
        refreshToken = token;
        if (persist) localStorage.setItem('refresh_token', token);
        else localStorage.removeItem('refresh_token');
    },
    clearTokens: () => {
        accessToken = null;
        refreshToken = null;
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('admin_user');
    }
};
