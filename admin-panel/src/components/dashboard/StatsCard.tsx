import type { LucideIcon } from 'lucide-react';
import { clsx } from 'clsx';

interface StatsCardProps {
    title: string;
    value: string;
    icon: LucideIcon;
    color: 'primary' | 'blue' | 'green' | 'yellow' | 'red' | 'purple';
    trend?: {
        value: number;
        isPositive: boolean;
    };
}

const colorMap = {
    primary: 'bg-primary-500',
    blue: 'bg-blue-500',
    green: 'bg-green-500',
    yellow: 'bg-yellow-500',
    red: 'bg-red-500',
    purple: 'bg-purple-500',
};

export const StatsCard: React.FC<StatsCardProps> = ({ title, value, icon: Icon, color, trend }) => {
    return (
        <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 flex items-start justify-between transition-all duration-300">
            <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 font-medium font-vazir">{title}</p>
                <h3 className="text-2xl font-bold mt-2 text-gray-900 dark:text-white font-vazir">{value}</h3>
                {trend && (
                    <p className={clsx(
                        "text-xs mt-2 flex items-center gap-1 font-vazir",
                        trend.isPositive ? "text-green-600" : "text-red-600"
                    )}>
                        {trend.isPositive ? '↑' : '↓'} %{trend.value} نسبت به دیروز
                    </p>
                )}
            </div>
            <div className={clsx("p-3 rounded-xl shadow-lg shadow-gray-200/50", colorMap[color])}>
                <Icon className="w-6 h-6 text-white" />
            </div>
        </div>
    );
};
