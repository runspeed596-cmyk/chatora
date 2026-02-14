import { api } from './api';
import type { SubscriptionPlan, UpdatePlanRequest } from '../types/subscription';

export const subscriptionService = {
    getPlans: async (): Promise<SubscriptionPlan[]> => {
        const response = await api.get('/admin/subscriptions');
        const data = response.data.data || [];
        return data.map((p: any) => ({
            id: p.id,
            name: p.name,
            durationMonths: p.durationMonths,
            price: p.price,
            currency: p.currency || 'USD',
            features: p.features || [],
            lastUpdated: p.lastUpdated
        }));
    },

    updatePlan: async (data: UpdatePlanRequest): Promise<SubscriptionPlan> => {
        const response = await api.put<SubscriptionPlan>(`/admin/plans/${data.id}`, data);
        return response.data;
    }
};
