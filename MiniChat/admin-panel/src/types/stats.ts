export interface StatsSummary {
    totalUsers: number;
    activeUsers: number;
    dailyVisits: number;
    totalRevenue: number;
    dailyIncome: number;
    monthlyRevenue: number;
}

export interface RevenueData {
    date: string;
    amount: number;
}

export interface CountryData {
    country: string;
    code: string;
    count: number;
}
