import React, { createContext, useContext, useEffect, useState } from 'react';
import { tokenManager } from '../utils/tokenManager';
import type { AuthState, LoginResponse } from '../types/auth'; // User removed if unused

interface AuthContextType extends AuthState {
    login: (data: LoginResponse) => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [state, setState] = useState<AuthState>({
        user: null, // We keep user in state, so we might need User type if we typed this strictly, but AuthState uses it.
        isAuthenticated: false,
        isLoading: true,
    });

    useEffect(() => {
        // Listen for forced logout (from api interceptor)
        const handleLogout = () => logout();
        window.addEventListener('auth:logout', handleLogout);

        setState(prev => ({ ...prev, isLoading: false }));

        return () => {
            window.removeEventListener('auth:logout', handleLogout);
        };
    }, []);

    const login = (data: LoginResponse) => {
        tokenManager.setAccessToken(data.accessToken);
        tokenManager.setRefreshToken(data.refreshToken);
        setState({
            user: data.user,
            isAuthenticated: true,
            isLoading: false,
        });
    };

    const logout = () => {
        tokenManager.clearTokens();
        setState({
            user: null,
            isAuthenticated: false,
            isLoading: false,
        });
    };

    return (
        <AuthContext.Provider value={{ ...state, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
