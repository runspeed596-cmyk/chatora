import { api } from './api';
import type { Transaction, RevenueStats } from '../types/revenue';

export const revenueService = {
    getStats: async (): Promise<RevenueStats> => {
        const response = await api.get('/admin/stats');
        const data = response.data.data;
        return {
            totalIncome: data.totalRevenue || 0,
            dailyIncome: data.dailyIncome || 0,
            monthlyIncome: data.monthlyRevenue || 0,
            successfulPayments: data.successfulPayments || 0,
            failedPayments: data.failedPayments || 0
        };
    },

    getTransactions: async (): Promise<Transaction[]> => {
        const response = await api.get('/admin/transactions');
        return response.data.data || [];
    }
};
