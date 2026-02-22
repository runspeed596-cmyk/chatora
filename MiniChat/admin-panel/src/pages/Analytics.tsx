import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Globe, Users } from 'lucide-react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { statsService } from '../services/statsService';
import { StatsCard } from '../components/dashboard/StatsCard';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4'];

export const Analytics: React.FC = () => {
    const { data: summary } = useQuery({ queryKey: ['stats-summary'], queryFn: statsService.getSummary });
    const { data: countries, isLoading } = useQuery({ queryKey: ['countries'], queryFn: statsService.getTopCountries });

    if (isLoading) return <div className="p-8 text-center font-vazir text-gray-400">در حال بارگذاری آمار جغرافیایی...</div>;

    return (
        <div className="space-y-8 animate-in fade-in duration-500">
            <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-vazir">تحلیل جغرافیایی</h2>
                <div className="flex items-center gap-2 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400 px-4 py-2 rounded-lg text-sm font-medium font-vazir border border-blue-100 dark:border-blue-900/30">
                    <Globe className="w-4 h-4" />
                    پراکنش جهانی کاربران
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                {/* Stats */}
                <div className="md:col-span-1 space-y-6">
                    <StatsCard
                        title="تعداد کل کشورها"
                        value={countries?.length.toString() || '0'}
                        icon={Globe}
                        color="blue"
                    />
                    <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700">
                        <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-6 font-vazir">خلاصه وضعیت</h3>
                        <div className="space-y-4">
                            <div className="flex items-center justify-between text-sm">
                                <span className="text-gray-500 dark:text-gray-400 font-vazir">تمرکز اصلی:</span>
                                <span className="font-bold text-gray-900 dark:text-white font-vazir">{countries?.[0]?.country || 'نامشخص'}</span>
                            </div>
                            <div className="flex items-center justify-between text-sm">
                                <span className="text-gray-500 dark:text-gray-400 font-vazir">میانگین کاربر هر کشور:</span>
                                <span className="font-bold text-gray-900 dark:text-white font-vazir">
                                    {countries && summary ? Math.round(summary.totalUsers / countries.length).toLocaleString() : 0}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Chart */}
                <div className="md:col-span-2 bg-white dark:bg-slate-800 p-8 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700">
                    <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-8 font-vazir text-center">نمودار توزیع جغرافیایی</h3>
                    <div className="h-80 w-full" dir="ltr">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={countries}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={80}
                                    outerRadius={120}
                                    paddingAngle={5}
                                    dataKey="count"
                                    nameKey="country"
                                >
                                    {countries?.map((_, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    contentStyle={{ borderRadius: '12px', border: 'none', fontFamily: 'Vazirmatn' }}
                                    formatter={(value: any) => [`${value.toLocaleString()} کاربر`, 'تعداد']}
                                />
                                <Legend wrapperStyle={{ fontFamily: 'Vazirmatn', paddingTop: '20px' }} />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </div>

            {/* Detailed Table */}
            <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 overflow-hidden">
                <div className="p-6 border-b border-gray-100 dark:border-slate-700 flex items-center justify-between">
                    <h3 className="text-lg font-bold text-gray-900 dark:text-white font-vazir">لیست تفکیکی کشورها</h3>
                    <Users className="w-5 h-5 text-gray-400" />
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full text-right" dir="rtl">
                        <thead className="bg-gray-50 dark:bg-slate-900/50">
                            <tr>
                                <th className="px-6 py-4 text-sm font-bold text-gray-500 dark:text-gray-400 font-vazir">ردیف</th>
                                <th className="px-6 py-4 text-sm font-bold text-gray-500 dark:text-gray-400 font-vazir">کشور</th>
                                <th className="px-6 py-4 text-sm font-bold text-gray-500 dark:text-gray-400 font-vazir">تعداد کاربر</th>
                                <th className="px-6 py-4 text-sm font-bold text-gray-500 dark:text-gray-400 font-vazir">درصد از کل</th>
                                <th className="px-6 py-4 text-sm font-bold text-gray-500 dark:text-gray-400 font-vazir">نمودار پیشرفت</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 dark:divide-slate-700">
                            {countries?.map((c, index) => (
                                <tr key={c.code} className="hover:bg-gray-50 dark:hover:bg-slate-700/50 transition-colors">
                                    <td className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400 font-vazir">{index + 1}</td>
                                    <td className="px-6 py-4 font-bold text-gray-900 dark:text-white font-vazir">{c.country}</td>
                                    <td className="px-6 py-4 text-sm text-gray-700 dark:text-gray-300 font-vazir">{c.count.toLocaleString()}</td>
                                    <td className="px-6 py-4 text-sm text-gray-600 dark:text-gray-400 font-mono" dir="ltr">
                                        {((c.count / (summary?.totalUsers || 1)) * 100).toFixed(1)}%
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="w-full bg-gray-100 dark:bg-slate-700 h-2 rounded-full overflow-hidden min-w-[120px]">
                                            <div
                                                className="h-full rounded-full transition-all duration-1000 shadow-[0_0_8px_rgba(59,130,246,0.3)]"
                                                style={{
                                                    width: `${(c.count / (summary?.totalUsers || 1)) * 100}%`,
                                                    backgroundColor: COLORS[index % COLORS.length]
                                                }}
                                            />
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};
