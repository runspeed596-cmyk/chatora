import type { User as AuthUser } from './auth';

export interface User extends AuthUser {
    email: string;
    phoneNumber?: string;
    status: 'ACTIVE' | 'BLOCKED' | 'SUSPENDED';
    subscriptionType: 'FREE' | 'PREMIUM' | 'MONTHLY' | 'SIX_MONTHS' | 'YEARLY';
    isPremium: boolean;
    premiumUntil?: string; // ISO Date
    subscriptionExpiry?: string; // ISO Date
    lastLogin: string;
    registrationDate: string;
    ipAddress?: string;
    avatarUrl?: string;
}

export interface UserFilter {
    search?: string;
    status?: string;
    page: number;
    limit: number;
}

export interface UserListResponse {
    users: User[];
    total: number;
    page: number;
    totalPages: number;
}

export interface CreateUserRequest {
    username: string;
    email?: string;
    password: string;
    role: 'ADMIN' | 'USER';
    gender: 'MALE' | 'FEMALE' | 'UNSPECIFIED';
}
