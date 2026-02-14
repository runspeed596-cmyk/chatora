import React, { createContext, useContext, useEffect, useState } from 'react';
import { tokenManager } from '../utils/tokenManager';
import type { AuthState, LoginResponse } from '../types/auth';

interface AuthContextType extends AuthState {
    login: (data: LoginResponse) => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [state, setState] = useState<AuthState>({
        user: null,
        isAuthenticated: false,
        isLoading: true,
    });

    useEffect(() => {
        // Attempt to restore session from localStorage
        const storedUser = localStorage.getItem('admin_user');
        const token = tokenManager.getAccessToken();

        if (storedUser && token) {
            try {
                setState({
                    user: JSON.parse(storedUser),
                    isAuthenticated: true,
                    isLoading: false
                });
            } catch (e) {
                tokenManager.clearTokens();
                setState(prev => ({ ...prev, isLoading: false }));
            }
        } else {
            setState(prev => ({ ...prev, isLoading: false }));
        }

        // Listen for forced logout (e.g., 401 from API)
        const handleLogout = () => logout();
        window.addEventListener('auth:logout', handleLogout);

        return () => {
            window.removeEventListener('auth:logout', handleLogout);
        };
    }, []);

    const login = (data: LoginResponse) => {
        // Always persist tokens and user data to localStorage
        tokenManager.setAccessToken(data.accessToken);
        tokenManager.setRefreshToken(data.refreshToken);
        localStorage.setItem('admin_user', JSON.stringify(data.user));
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
