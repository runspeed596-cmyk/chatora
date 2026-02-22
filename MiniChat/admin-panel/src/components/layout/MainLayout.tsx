import React from 'react';
import { Outlet, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
    LayoutDashboard,
    Users as UsersIcon,
    CreditCard as SubscriptionIcon,
    Radio,
    LogOut,
    Menu,
    ChevronLeft
} from 'lucide-react';
import { clsx } from 'clsx';
import { ThemeToggle } from '../ui/ThemeToggle';

export const MainLayout: React.FC = () => {
    const { logout } = useAuth();
    const location = useLocation();
    const [isSidebarOpen, setIsSidebarOpen] = React.useState(false);

    const navigation = [
        { name: 'داشبورد', href: '/', icon: LayoutDashboard },
        { name: 'کاربران', href: '/users', icon: UsersIcon },
        { name: 'اشتراک‌ها', href: '/subscriptions', icon: SubscriptionIcon },
        { name: 'چت‌های فعال', href: '/active-chats', icon: Radio },
    ];

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-slate-950 flex shadow-inner" dir="rtl">
            {/* Mobile Sidebar Overlay */}
            {isSidebarOpen && (
                <div
                    className="fixed inset-0 z-40 bg-gray-900/50 lg:hidden backdrop-blur-sm transition-opacity"
                    onClick={() => setIsSidebarOpen(false)}
                />
            )}

            {/* Sidebar */}
            <aside className={clsx(
                "fixed inset-y-0 right-0 z-50 w-72 bg-white dark:bg-slate-900 shadow-2xl transform transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:inset-0 border-l border-gray-100 dark:border-slate-800",
                isSidebarOpen ? "translate-x-0" : "translate-x-full"
            )}>
                <div className="flex items-center gap-3 px-6 h-20 border-b border-gray-100 dark:border-slate-800">
                    <div className="w-10 h-10 bg-primary-600 rounded-xl flex items-center justify-center shadow-lg shadow-primary-200 dark:shadow-primary-900/30">
                        <LayoutDashboard className="w-6 h-6 text-white" />
                    </div>
                    <h1 className="text-xl font-black text-gray-900 dark:text-white font-vazir tracking-tight">پنل مینی‌چت</h1>
                </div>

                <div className="flex flex-col h-[calc(100%-80px)] justify-between">
                    <nav className="p-4 space-y-2">
                        {navigation.map((item) => {
                            const isActive = location.pathname === item.href;
                            return (
                                <Link
                                    key={item.name}
                                    to={item.href}
                                    onClick={() => setIsSidebarOpen(false)}
                                    className={clsx(
                                        "flex items-center justify-between px-4 py-3.5 rounded-xl transition-all duration-200 group font-vazir",
                                        isActive
                                            ? "bg-primary-600 text-white shadow-lg shadow-primary-200 dark:shadow-primary-900/30"
                                            : "text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-slate-800 hover:text-gray-900 dark:hover:text-white"
                                    )}
                                >
                                    <div className="flex items-center gap-3">
                                        <item.icon className={clsx("w-5 h-5", isActive ? "text-white" : "text-gray-400 dark:text-gray-500 group-hover:text-primary-600 dark:group-hover:text-primary-400")} />
                                        <span className="font-bold">{item.name}</span>
                                    </div>
                                    {isActive && <ChevronLeft className="w-4 h-4 text-white/70" />}
                                </Link>
                            );
                        })}
                    </nav>

                    <div className="p-4 border-t border-gray-100 dark:border-slate-800 bg-gray-50/50 dark:bg-slate-900/50 space-y-3">
                        <div className="flex items-center justify-between px-4 py-2 bg-white dark:bg-slate-800 rounded-xl border border-gray-100 dark:border-slate-700">
                            <span className="text-sm font-bold text-gray-700 dark:text-gray-300 font-vazir">حالت شب</span>
                            <ThemeToggle />
                        </div>
                        <button
                            onClick={logout}
                            className="flex items-center gap-3 w-full px-4 py-3 text-sm font-bold text-red-600 dark:text-red-400 rounded-xl hover:bg-red-50 dark:hover:bg-red-900/20 transition-all font-vazir"
                        >
                            <LogOut className="w-5 h-5" />
                            <span>خروج از حساب</span>
                        </button>
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
                {/* Mobile Header */}
                <header className="lg:hidden flex items-center justify-between p-4 bg-white dark:bg-slate-900 border-b border-gray-200 dark:border-slate-800 shadow-sm">
                    <button onClick={() => setIsSidebarOpen(true)} className="p-2 -mr-2 text-gray-600 dark:text-gray-400 active:scale-95 transition-transform">
                        <Menu className="w-6 h-6" />
                    </button>
                    <span className="font-black text-gray-900 dark:text-white font-vazir">پنل مینی‌چت</span>
                    <div className="w-10 h-10 bg-primary-50 dark:bg-primary-900/20 rounded-lg" />
                </header>

                <main className="flex-1 overflow-auto bg-gray-50/30 dark:bg-slate-950">
                    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-10">
                        <Outlet />
                    </div>
                </main>
            </div>
        </div>
    );
};
