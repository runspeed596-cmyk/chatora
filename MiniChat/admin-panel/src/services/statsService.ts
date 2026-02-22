import { api } from './api';
import type { StatsSummary, RevenueData, CountryData } from '../types/stats';

export const statsService = {
    getSummary: async (): Promise<StatsSummary> => {
        const response = await api.get('/admin/stats');
        const data = response.data.data;
        return {
            totalUsers: data.totalUsers,
            activeUsers: data.dailyActiveUsers,
            dailyVisits: data.dailyVisits,
            totalRevenue: data.totalRevenue,
            dailyIncome: data.dailyIncome,
            monthlyRevenue: data.monthlyRevenue
        };
    },

    getRevenueHistory: async (): Promise<RevenueData[]> => {
        const response = await api.get('/admin/stats/revenue-history');
        return response.data.data || [];
    },

    getTopCountries: async (): Promise<CountryData[]> => {
        const response = await api.get('/admin/stats/countries');
        return response.data.data || [];
    }
};
