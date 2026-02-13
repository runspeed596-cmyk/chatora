import React from 'react';
import { format } from 'date-fns-jalali';
import { ArrowUpRight, ArrowDownLeft } from 'lucide-react';
import { clsx } from 'clsx';
import type { Transaction } from '../../types/revenue';

interface TransactionTableProps {
    transactions: Transaction[];
    isLoading: boolean;
}

export const TransactionTable: React.FC<TransactionTableProps> = ({ transactions, isLoading }) => {
    if (isLoading) return <div className="p-8 text-center text-gray-500">در حال دریافت تراکنش‌ها...</div>;

    return (
        <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 dark:divide-slate-700">
                <thead className="bg-gray-50 dark:bg-slate-900/50">
                    <tr>
                        <th className="px-6 py-3 text-right text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">شناسه سفارش</th>
                        <th className="px-6 py-3 text-right text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">کاربر</th>
                        <th className="px-6 py-3 text-right text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">مبلغ</th>
                        <th className="px-6 py-3 text-right text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">وضعیت</th>
                        <th className="px-6 py-3 text-right text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">درگاه</th>
                        <th className="px-6 py-3 text-right text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider font-vazir">تاریخ</th>
                    </tr>
                </thead>
                <tbody className="bg-white dark:bg-slate-800 divide-y divide-gray-200 dark:divide-slate-700">
                    {transactions.map((txn) => (
                        <tr key={txn.id} className="hover:bg-gray-50 dark:hover:bg-slate-700/50 transition-colors">
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-500 dark:text-gray-400">{txn.orderId}</td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-900 dark:text-white font-vazir">{txn.username}</td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-900 dark:text-white font-vazir">
                                {txn.amount.toLocaleString()} {txn.currency === 'USDT' ? '$' : txn.currency}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                                <span className={clsx(
                                    "px-2.5 py-0.5 inline-flex text-xs leading-5 font-bold rounded-full items-center gap-1 font-vazir shadow-sm",
                                    txn.status === 'SUCCESS' ? "bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300" : "bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300"
                                )}>
                                    {txn.status === 'SUCCESS' ? (
                                        <><ArrowDownLeft className="w-3 h-3" /> موفق</>
                                    ) : (
                                        <><ArrowUpRight className="w-3 h-3" /> ناموفق</>
                                    )}
                                </span>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400 font-vazir">{txn.paymentMethod}</td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400 font-mono" dir="ltr">
                                {format(new Date(txn.date), 'yyyy/MM/dd HH:mm')}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div >
    );
};
