export interface Transaction {
    id: string;
    userId: string;
    username: string;
    amount: number;
    currency: string;
    status: 'SUCCESS' | 'FAILED' | 'PENDING';
    date: string;
    paymentMethod: string;
    orderId: string;
}

export interface RevenueStats {
    totalIncome: number;
    dailyIncome: number;
    monthlyIncome: number;
    successfulPayments: number;
    failedPayments: number;
}
