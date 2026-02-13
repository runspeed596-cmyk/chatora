let accessToken: string | null = null;
let refreshToken: string | null = null;

export const tokenManager = {
    getAccessToken: () => accessToken,
    getRefreshToken: () => refreshToken,
    setAccessToken: (token: string) => {
        accessToken = token;
    },
    setRefreshToken: (token: string) => {
        refreshToken = token;
        // Ideally, this should be stored in an HttpOnly cookie by the backend.
        // If we must persist it on the client without cookies, we are at risk of XSS.
        // We will use sessionStorage as a compromise for "session-only" persistence if needed, 
        // but per strict security rules ("No sensitive data in localStorage"), 
        // we will strictly keep it in memory. User will need to login on refresh.
        // If persistence is required, we need a backend change.
    },
    clearTokens: () => {
        accessToken = null;
        refreshToken = null;
    }
};
