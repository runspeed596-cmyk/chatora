import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { Radio, Users, Clock, RefreshCw } from 'lucide-react';

interface ActiveMatch {
    matchId: string;
    user1: string;
    user2: string;
    startedAt: number;
}

const formatDuration = (startedAt: number): string => {
    if (!startedAt) return '—';
    const seconds = Math.floor((Date.now() - startedAt) / 1000);
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    if (minutes < 60) return `${minutes}m ${secs}s`;
    const hours = Math.floor(minutes / 60);
    return `${hours}h ${minutes % 60}m`;
};

export const ActiveChats: React.FC = () => {
    const { data, isLoading, error, refetch, dataUpdatedAt } = useQuery({
        queryKey: ['active-chats'],
        queryFn: async () => {
            const response = await api.get('/admin/active-chats');
            const payload = response.data;
            // Handle ApiResponse wrapper: { success: boolean, data: [...], message: string }
            if (payload && typeof payload === 'object' && 'data' in payload && Array.isArray(payload.data)) {
                return payload.data as ActiveMatch[];
            }
            // Fallback: response is the array directly
            if (Array.isArray(payload)) {
                return payload as ActiveMatch[];
            }
            console.warn('Unexpected active-chats response format:', payload);
            return [];
        },
        refetchInterval: 3000, // Auto-refresh every 3 seconds
        refetchIntervalInBackground: true,
        retry: 2,
    });

    const matches = data || [];

    // Force re-render every second for duration counter
    const [, setTick] = React.useState(0);
    React.useEffect(() => {
        const timer = setInterval(() => setTick(t => t + 1), 1000);
        return () => clearInterval(timer);
    }, []);

    return (
        <div className="space-y-6" dir="rtl">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <div className="relative">
                        <Radio className="w-7 h-7 text-red-500" />
                        <span className="absolute -top-0.5 -right-0.5 w-3 h-3 bg-red-500 rounded-full animate-ping" />
                        <span className="absolute -top-0.5 -right-0.5 w-3 h-3 bg-red-500 rounded-full" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-black text-gray-900 dark:text-white font-vazir">
                            چت‌های فعال
                        </h1>
                        <p className="text-sm text-gray-500 dark:text-gray-400 font-vazir">
                            نمایش زنده تماس‌های ویدیویی در حال انجام
                        </p>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    <div className="flex items-center gap-2 px-4 py-2 bg-red-50 dark:bg-red-900/20 rounded-xl border border-red-200 dark:border-red-800">
                        <Users className="w-5 h-5 text-red-600 dark:text-red-400" />
                        <span className="text-lg font-black text-red-600 dark:text-red-400">{matches.length}</span>
                        <span className="text-sm font-bold text-red-500 dark:text-red-400 font-vazir">تماس فعال</span>
                    </div>
                    <button
                        onClick={() => refetch()}
                        className="p-2.5 rounded-xl bg-white dark:bg-slate-800 border border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-700 transition-colors"
                        title="بروزرسانی"
                    >
                        <RefreshCw className="w-5 h-5 text-gray-500 dark:text-gray-400" />
                    </button>
                </div>
            </div>

            {/* Info Banner */}
            <div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-xl p-4">
                <p className="text-sm text-amber-700 dark:text-amber-300 font-vazir">
                    ⚡ این داده‌ها <strong>موقتی</strong> هستند و فقط در حافظه سرور ذخیره می‌شوند. هیچ چیز در دیتابیس ثبت نمی‌شود. با فشردن findNext توسط کاربر، تماس بلافاصله از لیست حذف می‌شود.
                </p>
            </div>

            {/* Table */}
            {isLoading ? (
                <div className="flex items-center justify-center py-20">
                    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
                </div>
            ) : error ? (
                <div className="text-center py-20">
                    <p className="text-red-500 font-vazir">خطا در بارگذاری داده‌ها</p>
                    <p className="text-red-400 text-sm mt-2 font-mono" dir="ltr">
                        {error instanceof Error ? error.message : String(error)}
                    </p>
                    <button
                        onClick={() => refetch()}
                        className="mt-4 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg font-vazir transition-colors"
                    >
                        تلاش مجدد
                    </button>
                </div>
            ) : matches.length === 0 ? (
                <div className="text-center py-20 bg-white dark:bg-slate-900 rounded-2xl border border-gray-100 dark:border-slate-800">
                    <Radio className="w-16 h-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
                    <h3 className="text-lg font-bold text-gray-500 dark:text-gray-400 font-vazir">
                        هیچ تماس فعالی وجود ندارد
                    </h3>
                    <p className="text-sm text-gray-400 dark:text-gray-500 mt-1 font-vazir">
                        وقتی کاربران تماس ویدیویی برقرار کنند، اینجا نمایش داده می‌شود
                    </p>
                </div>
            ) : (
                <div className="bg-white dark:bg-slate-900 rounded-2xl border border-gray-100 dark:border-slate-800 overflow-hidden shadow-sm">
                    <table className="w-full">
                        <thead>
                            <tr className="border-b border-gray-100 dark:border-slate-800 bg-gray-50 dark:bg-slate-800/50">
                                <th className="px-6 py-4 text-right text-xs font-black text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">شناسه</th>
                                <th className="px-6 py-4 text-right text-xs font-black text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">کاربر ۱</th>
                                <th className="px-6 py-4 text-right text-xs font-black text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">کاربر ۲</th>
                                <th className="px-6 py-4 text-right text-xs font-black text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">مدت تماس</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 dark:divide-slate-800">
                            {matches.map((match) => (
                                <tr key={match.matchId} className="hover:bg-gray-50 dark:hover:bg-slate-800/50 transition-colors">
                                    <td className="px-6 py-4">
                                        <span className="text-xs font-mono bg-gray-100 dark:bg-slate-800 px-2 py-1 rounded text-gray-600 dark:text-gray-400">
                                            {match.matchId.substring(0, 8)}…
                                        </span>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2">
                                            <div className="w-8 h-8 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                                                <span className="text-green-600 dark:text-green-400 text-sm font-bold">
                                                    {match.user1.charAt(0).toUpperCase()}
                                                </span>
                                            </div>
                                            <span className="font-bold text-gray-900 dark:text-white">{match.user1}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2">
                                            <div className="w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
                                                <span className="text-blue-600 dark:text-blue-400 text-sm font-bold">
                                                    {match.user2.charAt(0).toUpperCase()}
                                                </span>
                                            </div>
                                            <span className="font-bold text-gray-900 dark:text-white">{match.user2}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2">
                                            <Clock className="w-4 h-4 text-gray-400" />
                                            <span className="text-sm font-bold text-gray-700 dark:text-gray-300 font-mono">
                                                {formatDuration(match.startedAt)}
                                            </span>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Auto-refresh indicator */}
            <div className="text-center">
                <span className="text-xs text-gray-400 dark:text-gray-500 font-vazir">
                    بروزرسانی خودکار هر ۳ ثانیه • آخرین بروزرسانی: {new Date(dataUpdatedAt || Date.now()).toLocaleTimeString('fa-IR')}
                </span>
            </div>
        </div>
    );
};
