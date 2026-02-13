import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Banknote, TrendingUp, TrendingDown, CreditCard } from 'lucide-react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { revenueService } from '../services/revenueService';
import { statsService } from '../services/statsService';
import { StatsCard } from '../components/dashboard/StatsCard';
import { TransactionTable } from '../components/revenue/TransactionTable';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8'];

export const Revenue: React.FC = () => {
    const { data: stats } = useQuery({ queryKey: ['revenueStats'], queryFn: revenueService.getStats });
    const { data: transactions, isLoading: txLoading } = useQuery({ queryKey: ['transactions'], queryFn: revenueService.getTransactions });
    const { data: countries } = useQuery({ queryKey: ['topCountries'], queryFn: statsService.getTopCountries });

    return (
        <div className="space-y-6">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-vazir">درآمد و آمار</h2>

            {/* Revenue Stats */}
            {stats && (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <StatsCard
                        title="درآمد کل"
                        value={(stats.totalIncome / 1).toLocaleString() + ' $'}
                        icon={Banknote}
                        color="primary"
                    />
                    <StatsCard
                        title="درآمد ماهانه"
                        value={(stats.monthlyIncome / 1).toLocaleString() + ' $'}
                        icon={TrendingUp}
                        color="blue"
                    />
                    <StatsCard
                        title="تراکنش‌های موفق"
                        value={stats.successfulPayments.toLocaleString()}
                        icon={CreditCard}
                        color="primary"
                    />
                    <StatsCard
                        title="تراکنش‌های ناموفق"
                        value={stats.failedPayments.toLocaleString()}
                        icon={TrendingDown}
                        color="red"
                    />
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Transactions List */}
                <div className="lg:col-span-2 bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 p-6">
                    <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4 font-vazir">تراکنش‌های اخیر</h3>
                    <TransactionTable transactions={transactions || []} isLoading={txLoading} />
                </div>

                {/* Country Stats */}
                <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 p-6">
                    <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4 font-vazir">کاربران بر اساس IP</h3>
                    <div className="h-[300px] w-full" dir="ltr">
                        <ResponsiveContainer>
                            <PieChart>
                                <Pie
                                    data={countries}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={80}
                                    paddingAngle={5}
                                    dataKey="count"
                                    nameKey="country"
                                >
                                    {countries?.map((_, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip />
                                <Legend />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                    <div className="mt-4 space-y-2">
                        {countries?.map((country, index) => (
                            <div key={country.code} className="flex items-center justify-between text-sm">
                                <span className="flex items-center gap-2 text-gray-700 dark:text-gray-300 font-vazir">
                                    <span className="w-3 h-3 rounded-full" style={{ backgroundColor: COLORS[index % COLORS.length] }} />
                                    {country.country}
                                </span>
                                <span className="font-semibold text-gray-900 dark:text-white font-vazir">{country.count}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};
