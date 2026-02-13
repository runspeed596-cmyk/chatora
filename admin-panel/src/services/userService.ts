import { api } from './api';
import type { UserFilter, UserListResponse } from '../types/user';

export const userService = {
    getUsers: async (filter: UserFilter): Promise<UserListResponse> => {
        const params = new URLSearchParams();
        if (filter.search) params.append('search', filter.search);
        if (filter.status && filter.status !== 'ALL') params.append('status', filter.status);
        params.append('page', filter.page.toString());
        params.append('limit', filter.limit.toString());

        const response = await api.get('/admin/users', { params });
        return response.data.data;
    },

    blockUser: async (userId: string): Promise<void> => {
        await api.post(`/admin/users/${userId}/block`);
    },

    unblockUser: async (userId: string): Promise<void> => {
        await api.post(`/admin/users/${userId}/unblock`);
    },

    deleteUser: async (userId: string): Promise<void> => {
        await api.delete(`/admin/users/${userId}`);
    }
};
