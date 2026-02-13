import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userService } from '../services/userService';
import {
    Search,
    UserX,
    UserCheck,
    Trash2
} from 'lucide-react';
import { clsx } from 'clsx';
import { format } from 'date-fns-jalali';

export const Users: React.FC = () => {
    const queryClient = useQueryClient();
    const [page, setPage] = useState(1);
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState<string>('ALL');

    const { data, isLoading } = useQuery({
        queryKey: ['users', page, search, status],
        queryFn: () => userService.getUsers({
            page,
            limit: 10,
            search: search || undefined,
            status: status === 'ALL' ? undefined : status
        })
    });

    const blockMutation = useMutation({
        mutationFn: userService.blockUser,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['users'] });
            alert('کاربر با موفقیت مسدود شد');
        }
    });

    const unblockMutation = useMutation({
        mutationFn: userService.unblockUser,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['users'] });
            alert('رفع مسدودیت انجام شد');
        }
    });

    const deleteMutation = useMutation({
        mutationFn: userService.deleteUser,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['users'] });
            alert('کاربر حذف شد');
        }
    });

    const handleBlockAction = (userId: string, isBlocked: boolean) => {
        if (isBlocked) {
            unblockMutation.mutate(userId);
        } else {
            if (window.confirm('آیا از مسدود کردن این کاربر اطمینان دارید؟')) {
                blockMutation.mutate(userId);
            }
        }
    };

    return (
        <div className="space-y-6 animate-in fade-in duration-500">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-vazir">مدیریت کاربران</h2>

                <div className="flex flex-wrap items-center gap-3">
                    <div className="relative">
                        <Search className="w-5 h-5 absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" />
                        <input
                            type="text"
                            placeholder="جستجوی کاربر..."
                            value={search}
                            onChange={(e) => {
                                setSearch(e.target.value);
                                setPage(1);
                            }}
                            className="pr-10 pl-4 py-2 border border-gray-200 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none w-64 font-vazir bg-white dark:bg-slate-800 text-gray-900 dark:text-white transition-all shadow-sm"
                        />
                    </div>

                    <select
                        value={status}
                        onChange={(e) => {
                            setStatus(e.target.value);
                            setPage(1);
                        }}
                        className="px-4 py-2 border border-gray-200 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none bg-white dark:bg-slate-800 text-gray-900 dark:text-white font-vazir transition-all shadow-sm"
                    >
                        <option value="ALL">همه وضعیت‌ها</option>
                        <option value="ACTIVE">فعال</option>
                        <option value="BLOCKED">مسدود شده</option>
                    </select>
                </div>
            </div>

            <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 overflow-hidden">
                <div className="overflow-x-auto text-right" dir="rtl">
                    <table className="w-full">
                        <thead className="bg-gray-50 dark:bg-slate-900/50 border-b border-gray-100 dark:border-slate-700">
                            <tr>
                                <th className="px-6 py-4 text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">کاربر</th>
                                <th className="px-6 py-4 text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">نقش</th>
                                <th className="px-6 py-4 text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">وضیعت</th>
                                <th className="px-6 py-4 text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">اشتراک</th>
                                <th className="px-6 py-4 text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">تاریخ عضویت</th>
                                <th className="px-6 py-4 text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir text-center">عملیات</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 dark:divide-slate-700">
                            {isLoading ? (
                                Array(5).fill(0).map((_, i) => (
                                    <tr key={i} className="animate-pulse">
                                        <td colSpan={6} className="px-6 py-8">
                                            <div className="h-4 bg-gray-100 dark:bg-slate-700 rounded w-full" />
                                        </td>
                                    </tr>
                                ))
                            ) : data?.users.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="px-6 py-12 text-center text-gray-400 font-vazir">
                                        کاربری یافت نشد
                                    </td>
                                </tr>
                            ) : data?.users.map((user) => (
                                <tr key={user.id} className="hover:bg-gray-50 dark:hover:bg-slate-700/50 transition-colors">
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center text-primary-700 dark:text-primary-400 font-bold font-vazir">
                                                {user.username.charAt(0).toUpperCase()}
                                            </div>
                                            <div>
                                                <p className="font-bold text-gray-900 dark:text-white font-vazir">{user.username}</p>
                                                <p className="text-xs text-gray-500 dark:text-gray-400">{user.email || 'ایمیل ثبت نشده'}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={clsx(
                                            "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium font-vazir shadow-sm",
                                            user.role === 'ADMIN' ? "bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-300" : "bg-gray-100 dark:bg-slate-700 text-gray-800 dark:text-gray-300"
                                        )}>
                                            {user.role === 'ADMIN' ? 'مدیر' : 'کاربر'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={clsx(
                                            "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium font-vazir shadow-sm",
                                            user.status === 'ACTIVE' ? "bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300" : "bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300"
                                        )}>
                                            {user.status === 'ACTIVE' ? 'فعال' : 'مسدود'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 font-vazir">
                                        {user.subscriptionType !== 'FREE' ? <span className="text-primary-600 dark:text-primary-400 font-bold">ویژه ✨</span> : 'رایگان'}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400 font-vazir">
                                        {format(new Date(user.registrationDate), 'yyyy/MM/dd')}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="flex items-center justify-center gap-2">
                                            <button
                                                onClick={() => handleBlockAction(user.id, user.status === 'BLOCKED')}
                                                className={clsx(
                                                    "p-2 rounded-lg transition-colors shadow-sm border border-transparent",
                                                    user.status === 'ACTIVE' ? "text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 dark:border-slate-700" : "text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20 dark:border-slate-700"
                                                )}
                                                title={user.status === 'ACTIVE' ? 'مسدود کردن' : 'رفع مسدودیت'}
                                            >
                                                {user.status === 'ACTIVE' ? <UserX className="w-5 h-5" /> : <UserCheck className="w-5 h-5" />}
                                            </button>
                                            <button
                                                onClick={() => {
                                                    if (window.confirm('آیا از حذف دائمی این کاربر اطمینان دارید؟')) {
                                                        deleteMutation.mutate(user.id);
                                                    }
                                                }}
                                                className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 dark:border-slate-700 rounded-lg transition-colors shadow-sm"
                                                title="حذف کاربر"
                                            >
                                                <Trash2 className="w-5 h-5" />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                <div className="px-6 py-4 border-t border-gray-100 dark:border-slate-700 flex items-center justify-between bg-gray-50/30 dark:bg-slate-900/30">
                    <span className="text-sm text-gray-500 dark:text-gray-400 font-vazir">
                        نمایش {((page - 1) * 10) + 1} تا {Math.min(page * 10, data?.total || 0)} از {data?.total || 0} کاربر
                    </span>
                    <div className="flex gap-2">
                        <button
                            onClick={() => setPage(p => Math.max(1, p - 1))}
                            disabled={page === 1 || isLoading}
                            className="px-4 py-2 border border-gray-200 dark:border-slate-700 rounded-lg text-sm font-medium disabled:opacity-50 hover:bg-gray-50 dark:hover:bg-slate-700 transition-all font-vazir text-gray-700 dark:text-gray-300"
                        >
                            قبلی
                        </button>
                        <button
                            onClick={() => setPage(p => Math.min(data?.totalPages || 1, p + 1))}
                            disabled={page === data?.totalPages || isLoading}
                            className="px-4 py-2 border border-gray-200 dark:border-slate-700 rounded-lg text-sm font-medium disabled:opacity-50 hover:bg-gray-50 dark:hover:bg-slate-700 transition-all font-vazir text-gray-700 dark:text-gray-300"
                        >
                            بعدی
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};
