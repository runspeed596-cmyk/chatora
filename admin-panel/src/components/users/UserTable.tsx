import React from 'react';
import { format } from 'date-fns-jalali';
import { Ban, CheckCircle, Trash2 } from 'lucide-react';
import { clsx } from 'clsx';
import type { User } from '../../types/user';

interface UserTableProps {
    users: User[];
    isLoading: boolean;
    onBlock: (id: string, isBlocked: boolean) => void;
    onDelete: (id: string) => void;
}

export const UserTable: React.FC<UserTableProps> = ({ users, isLoading, onBlock, onDelete }) => {
    if (isLoading) {
        return <div className="p-8 text-center text-gray-500">در حال دریافت لیست کاربران...</div>;
    }

    if (users.length === 0) {
        return <div className="p-8 text-center text-gray-500">کاربری یافت نشد.</div>;
    }

    return (
        <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                    <tr>
                        <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                            کاربر
                        </th>
                        <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                            وضعیت
                        </th>
                        <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                            اشتراک
                        </th>
                        <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                            تاریخ عضویت
                        </th>
                        <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                            آخرین ورود
                        </th>
                        <th scope="col" className="relative px-6 py-3">
                            <span className="sr-only">اقدامات</span>
                        </th>
                    </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                    {users.map((user) => (
                        <tr key={user.id} className="hover:bg-gray-50 transition-colors">
                            <td className="px-6 py-4 whitespace-nowrap">
                                <div className="flex items-center">
                                    <div className="flex-shrink-0 h-10 w-10">
                                        <div className="h-10 w-10 rounded-full bg-primary-100 flex items-center justify-center text-primary-600 font-bold">
                                            {user.username.charAt(0).toUpperCase()}
                                        </div>
                                    </div>
                                    <div className="mr-4">
                                        <div className="text-sm font-medium text-gray-900">{user.username}</div>
                                        <div className="text-sm text-gray-500">{user.email}</div>
                                    </div>
                                </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                                <span className={clsx(
                                    "px-2 inline-flex text-xs leading-5 font-semibold rounded-full",
                                    user.status === 'ACTIVE'
                                        ? "bg-green-100 text-green-800"
                                        : "bg-red-100 text-red-800"
                                )}>
                                    {user.status === 'ACTIVE' ? 'فعال' : 'مسدود'}
                                </span>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                {user.subscriptionType === 'FREE' ? 'رایگان' : 'ویژه'}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500" dir="ltr">
                                {format(new Date(user.registrationDate), 'yyyy/MM/dd')}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500" dir="ltr">
                                {format(new Date(user.lastLogin), 'HH:mm - yyyy/MM/dd')}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-left text-sm font-medium">
                                <div className="flex items-center gap-2 justify-end">
                                    <button
                                        onClick={() => onBlock(user.id, user.status === 'ACTIVE')}
                                        className={clsx(
                                            "p-1 rounded hover:bg-gray-100 transition-colors",
                                            user.status === 'ACTIVE' ? "text-red-600" : "text-green-600"
                                        )}
                                        title={user.status === 'ACTIVE' ? 'مسدود کردن' : 'رفع مسدودی'}
                                    >
                                        {user.status === 'ACTIVE' ? <Ban className="w-5 h-5" /> : <CheckCircle className="w-5 h-5" />}
                                    </button>
                                    <button
                                        onClick={() => onDelete(user.id)}
                                        className="p-1 rounded hover:bg-gray-100 text-gray-600 hover:text-red-600 transition-colors"
                                        title="حذف"
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
    );
};
