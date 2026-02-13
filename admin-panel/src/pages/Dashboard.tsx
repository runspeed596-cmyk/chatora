import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
    Users as UsersIcon,
    Activity,
    Banknote,
    Eye,
    MapPin
} from 'lucide-react';
import { StatsCard } from '../components/dashboard/StatsCard';
import { RevenueChart } from '../components/dashboard/RevenueChart';
import { statsService } from '../services/statsService';

export const Dashboard: React.FC = () => {
    const { data: summary, isLoading: isSummaryLoading } = useQuery({
        queryKey: ['stats-summary'],
        queryFn: statsService.getSummary,
        refetchInterval: 30000 // Refresh every 30s
    });

    const { data: revenueHistory, isLoading: isRevenueLoading } = useQuery({
        queryKey: ['revenue-history'],
        queryFn: statsService.getRevenueHistory
    });

    const { data: countries } = useQuery({
        queryKey: ['countries'],
        queryFn: statsService.getTopCountries
    });



    if (isSummaryLoading || isRevenueLoading) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    if (!summary) return null;

    return (
        <div className="space-y-8 animate-in fade-in duration-500">
            <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-vazir">نمای کلی داشبورد</h2>
                <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400 font-vazir">
                    <span className="relative flex h-2 w-2">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                        <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                    </span>
                    بروزرسانی زنده فعال است
                </div>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatsCard
                    title="کاربران کل"
                    value={summary.totalUsers.toLocaleString()}
                    icon={UsersIcon}
                    color="blue"
                    trend={{ value: 12, isPositive: true }}
                />
                <StatsCard
                    title="کاربران فعال (۲۴ ساعت)"
                    value={summary.activeUsers.toLocaleString()}
                    icon={Activity}
                    color="primary"
                    trend={{ value: 5, isPositive: true }}
                />
                <StatsCard
                    title="بازدید امروز"
                    value={summary.dailyVisits.toLocaleString()}
                    icon={Eye}
                    color="purple"
                />
                <StatsCard
                    title="درآمد کل"
                    value={`${summary.totalRevenue.toLocaleString()} $`}
                    icon={Banknote}
                    color="green"
                    trend={{ value: 8, isPositive: true }}
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Revenue Chart */}
                <div className="lg:col-span-2">
                    <RevenueChart data={revenueHistory || []} />
                </div>

                {/* Country Breakdown */}
                <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700">
                    <div className="flex items-center justify-between mb-8">
                        <h3 className="text-lg font-bold text-gray-900 dark:text-white font-vazir">توزیع کاربران (IP)</h3>
                        <MapPin className="w-5 h-5 text-gray-400" />
                    </div>
                    <div className="space-y-6">
                        {countries?.map((c) => (
                            <div key={c.code} className="space-y-2">
                                <div className="flex items-center justify-between text-sm">
                                    <span className="font-medium text-gray-700 dark:text-gray-300 font-vazir">{c.country}</span>
                                    <span className="text-gray-500 dark:text-gray-400 font-vazir">{c.count.toLocaleString()} کاربر</span>
                                </div>
                                <div className="w-full bg-gray-100 dark:bg-slate-700 h-2 rounded-full overflow-hidden">
                                    <div
                                        className="bg-primary-500 h-full rounded-full transition-all duration-1000"
                                        style={{ width: `${(c.count / (summary.totalUsers || 1)) * 100}%` }}
                                    />
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};
