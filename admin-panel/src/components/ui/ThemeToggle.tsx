import React from 'react';
import { Sun, Moon } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';
import { clsx } from 'clsx';

export const ThemeToggle: React.FC = () => {
    const { theme, toggleTheme } = useTheme();

    return (
        <button
            onClick={toggleTheme}
            className={clsx(
                "relative inline-flex h-9 w-16 items-center rounded-full transition-colors duration-300 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2",
                theme === 'dark' ? "bg-gray-700" : "bg-primary-100"
            )}
        >
            <span className="sr-only">تغییر تم</span>
            <span
                className={clsx(
                    "inline-block h-7 w-7 transform rounded-full bg-white shadow-lg transition-transform duration-300 flex items-center justify-center",
                    theme === 'dark' ? "-translate-x-8" : "-translate-x-1"
                )}
            >
                {theme === 'dark' ? (
                    <Moon className="h-4 w-4 text-gray-700" />
                ) : (
                    <Sun className="h-4 w-4 text-primary-600" />
                )}
            </span>
        </button>
    );
};
