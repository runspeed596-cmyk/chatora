import type { User as AuthUser } from './auth';

export interface User extends AuthUser {
    email: string;
    phoneNumber?: string;
    status: 'ACTIVE' | 'BLOCKED' | 'SUSPENDED';
    subscriptionType: 'FREE' | 'MONTHLY' | 'SIX_MONTHS' | 'YEARLY';
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
