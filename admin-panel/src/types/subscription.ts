export interface SubscriptionPlan {
    id: string;
    name: string;
    durationMonths: number;
    price: number;
    currency: string;
    features: string[];
    lastUpdated: string;
}

export interface UpdatePlanRequest {
    id: string;
    price: number;
}
