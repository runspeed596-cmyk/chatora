import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userService } from '../services/userService';
import { Modal } from '../components/ui/Modal';
import type { CreateUserRequest } from '../types/user';
import {
    Search,
    UserX,
    UserCheck,
    Trash2,
    UserPlus,
    Crown,
    ChevronDown
} from 'lucide-react';
import { clsx } from 'clsx';
import { format } from 'date-fns-jalali';

export const Users: React.FC = () => {
    const queryClient = useQueryClient();
    const [page, setPage] = useState(1);
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState<string>('ALL');
    const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newUser, setNewUser] = useState<CreateUserRequest>({
        username: '',
        email: '',
        password: '',
        role: 'USER',
        gender: 'UNSPECIFIED'
    });

    const showToast = (message: string, type: 'success' | 'error' = 'success') => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 3000);
    };

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
        onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['users'] }); showToast('کاربر مسدود شد'); },
        onError: () => showToast('خطا در مسدودسازی', 'error')
    });

    const unblockMutation = useMutation({
        mutationFn: userService.unblockUser,
        onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['users'] }); showToast('رفع مسدودیت انجام شد'); },
        onError: () => showToast('خطا در رفع مسدودیت', 'error')
    });

    const deleteMutation = useMutation({
        mutationFn: userService.deleteUser,
        onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['users'] }); showToast('کاربر حذف شد'); },
        onError: () => showToast('خطا در حذف کاربر', 'error')
    });

    const upgradeMutation = useMutation({
        mutationFn: userService.upgradeUser,
        onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['users'] }); showToast('ارتقا به پرو انجام شد ✨'); },
        onError: () => showToast('خطا در ارتقا', 'error')
    });

    const downgradeMutation = useMutation({
        mutationFn: userService.downgradeUser,
        onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['users'] }); showToast('نزول به حالت رایگان انجام شد'); },
        onError: () => showToast('خطا در نزول', 'error')
    });

    const createMutation = useMutation({
        mutationFn: userService.createUser,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['users'] });
            showToast('کاربر جدید ساخته شد ✅');
            setIsCreateModalOpen(false);
            setNewUser({ username: '', email: '', password: '', role: 'USER', gender: 'UNSPECIFIED' });
        },
        onError: (err: any) => showToast(err?.response?.data?.message || 'خطا در ساخت کاربر', 'error')
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

    const handleCreateSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!newUser.username || !newUser.password) return;
        createMutation.mutate(newUser);
    };

    return (
        <div className="space-y-6 animate-in fade-in duration-500">
            {/* Toast */}
            {toast && (
                <div className={clsx(
                    "fixed top-6 left-1/2 -translate-x-1/2 z-[100] px-6 py-3 rounded-xl shadow-2xl text-sm font-bold font-vazir transition-all animate-in slide-in-from-top duration-300",
                    toast.type === 'success'
                        ? "bg-green-600 text-white"
                        : "bg-red-600 text-white"
                )}>
                    {toast.message}
                </div>
            )}

            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-vazir">مدیریت کاربران</h2>

                <div className="flex flex-wrap items-center gap-3">
                    <button
                        onClick={() => setIsCreateModalOpen(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded-lg transition-colors shadow-lg shadow-primary-500/20 font-vazir text-sm font-bold"
                    >
                        <UserPlus className="w-4 h-4" />
                        افزودن کاربر
                    </button>

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
                                <th className="px-6 py-4 text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">وضعیت</th>
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
                                        {user.isPremium ? (
                                            <span className="inline-flex items-center gap-1 text-amber-600 dark:text-amber-400 font-bold">
                                                <Crown className="w-4 h-4" />
                                                ویژه ✨
                                            </span>
                                        ) : 'رایگان'}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400 font-vazir">
                                        {format(new Date(user.registrationDate), 'yyyy/MM/dd')}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="flex items-center justify-center gap-1.5">
                                            {/* Upgrade / Downgrade */}
                                            {user.isPremium ? (
                                                <button
                                                    onClick={() => {
                                                        if (window.confirm('نزول به حالت رایگان؟')) downgradeMutation.mutate(user.id);
                                                    }}
                                                    className="p-2 text-amber-500 hover:text-amber-700 hover:bg-amber-50 dark:hover:bg-amber-900/20 rounded-lg transition-colors"
                                                    title="نزول به رایگان"
                                                >
                                                    <ChevronDown className="w-5 h-5" />
                                                </button>
                                            ) : (
                                                <button
                                                    onClick={() => upgradeMutation.mutate(user.id)}
                                                    className="p-2 text-amber-400 hover:text-amber-600 hover:bg-amber-50 dark:hover:bg-amber-900/20 rounded-lg transition-colors"
                                                    title="ارتقا به پرو"
                                                >
                                                    <Crown className="w-5 h-5" />
                                                </button>
                                            )}

                                            {/* Block / Unblock */}
                                            <button
                                                onClick={() => handleBlockAction(user.id, user.status === 'BLOCKED')}
                                                className={clsx(
                                                    "p-2 rounded-lg transition-colors",
                                                    user.status === 'ACTIVE' ? "text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20" : "text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20"
                                                )}
                                                title={user.status === 'ACTIVE' ? 'مسدود کردن' : 'رفع مسدودیت'}
                                            >
                                                {user.status === 'ACTIVE' ? <UserX className="w-5 h-5" /> : <UserCheck className="w-5 h-5" />}
                                            </button>

                                            {/* Delete */}
                                            <button
                                                onClick={() => {
                                                    if (window.confirm('آیا از حذف دائمی این کاربر اطمینان دارید؟')) {
                                                        deleteMutation.mutate(user.id);
                                                    }
                                                }}
                                                className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors"
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

            {/* Create User Modal */}
            <Modal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                title="افزودن کاربر جدید"
            >
                <form onSubmit={handleCreateSubmit} className="mt-4 space-y-4 font-vazir" dir="rtl">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">نام کاربری *</label>
                        <input
                            type="text"
                            value={newUser.username}
                            onChange={(e) => setNewUser(p => ({ ...p, username: e.target.value }))}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none bg-white dark:bg-slate-800 text-gray-900 dark:text-white"
                            placeholder="نام کاربری"
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">ایمیل</label>
                        <input
                            type="email"
                            value={newUser.email}
                            onChange={(e) => setNewUser(p => ({ ...p, email: e.target.value }))}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none bg-white dark:bg-slate-800 text-gray-900 dark:text-white"
                            placeholder="ایمیل (اختیاری)"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">رمز عبور *</label>
                        <input
                            type="password"
                            value={newUser.password}
                            onChange={(e) => setNewUser(p => ({ ...p, password: e.target.value }))}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none bg-white dark:bg-slate-800 text-gray-900 dark:text-white"
                            placeholder="رمز عبور"
                            required
                            minLength={6}
                        />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">نقش</label>
                            <select
                                value={newUser.role}
                                onChange={(e) => setNewUser(p => ({ ...p, role: e.target.value as 'ADMIN' | 'USER' }))}
                                className="w-full px-3 py-2 border border-gray-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none bg-white dark:bg-slate-800 text-gray-900 dark:text-white"
                            >
                                <option value="USER">کاربر عادی</option>
                                <option value="ADMIN">مدیر</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">جنسیت</label>
                            <select
                                value={newUser.gender}
                                onChange={(e) => setNewUser(p => ({ ...p, gender: e.target.value as 'MALE' | 'FEMALE' | 'UNSPECIFIED' }))}
                                className="w-full px-3 py-2 border border-gray-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none bg-white dark:bg-slate-800 text-gray-900 dark:text-white"
                            >
                                <option value="UNSPECIFIED">مشخص نشده</option>
                                <option value="MALE">مرد</option>
                                <option value="FEMALE">زن</option>
                            </select>
                        </div>
                    </div>

                    <div className="flex gap-3 justify-end mt-6">
                        <button
                            type="button"
                            onClick={() => setIsCreateModalOpen(false)}
                            className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-slate-700 rounded-lg hover:bg-gray-200 dark:hover:bg-slate-600 transition-colors"
                        >
                            انصراف
                        </button>
                        <button
                            type="submit"
                            disabled={createMutation.isPending}
                            className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50"
                        >
                            {createMutation.isPending ? 'در حال ذخیره...' : 'ساخت کاربر'}
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
};
