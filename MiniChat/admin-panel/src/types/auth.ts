export interface User {
    id: string;
    username: string;
    role: 'ADMIN' | 'USER';
    firstName?: string;
    lastName?: string;
}

export interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    user: User;
}

export interface AuthState {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
}
