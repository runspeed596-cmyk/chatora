import React from 'react';
import { Check, Edit2 } from 'lucide-react';
import { format } from 'date-fns-jalali';
// import { clsx } from 'clsx';
import type { SubscriptionPlan } from '../../types/subscription';

interface SubscriptionCardProps {
    plan: SubscriptionPlan;
    onEdit: (plan: SubscriptionPlan) => void;
}

export const SubscriptionCard: React.FC<SubscriptionCardProps> = ({ plan, onEdit }) => {
    return (
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 p-6 flex flex-col hover:shadow-md dark:hover:shadow-slate-900/50 transition-all duration-300">
            <div className="flex justify-between items-start mb-4">
                <div>
                    <h3 className="text-xl font-bold text-gray-900 dark:text-white font-vazir">{plan.name}</h3>
                    <div className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400 mt-1 font-vazir">
                        <span className="w-1.5 h-1.5 rounded-full bg-primary-500 animate-pulse" />
                        <span>بروزرسانی: {format(new Date(plan.lastUpdated), 'yyyy/MM/dd')}</span>
                    </div>
                </div>
                <div className="bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-400 px-3 py-1 rounded-full text-sm font-black font-vazir shadow-sm">
                    {plan.durationMonths} ماهه
                </div>
            </div>

            <div className="mb-6">
                <span className="text-4xl font-black text-gray-900 dark:text-white font-vazir">{plan.price.toLocaleString()}</span>
                <span className="text-gray-500 dark:text-gray-400 mr-1 font-bold font-vazir">{plan.currency}</span>
            </div>

            <div className="flex-1 space-y-3 mb-8">
                {plan.features.map((feature, index) => (
                    <div key={index} className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-300 font-vazir">
                        <Check className="w-4 h-4 text-green-500 dark:text-green-400" />
                        <span>{feature}</span>
                    </div>
                ))}
            </div>

            <button
                onClick={() => onEdit(plan)}
                className="flex items-center justify-center gap-2 w-full border-2 border-primary-600 text-primary-600 dark:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-900/20 font-bold py-2.5 rounded-xl transition-all font-vazir shadow-sm active:scale-[0.98]"
            >
                <Edit2 className="w-4 h-4" />
                ویرایش تعرفه
            </button>
        </div>
    );
};
