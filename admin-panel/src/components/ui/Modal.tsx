import React from 'react';
import { X } from 'lucide-react';
// import { clsx } from 'clsx';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title: string;
    children: React.ReactNode;
}

export const Modal: React.FC<ModalProps> = ({ isOpen, onClose, title, children }) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 overflow-y-auto">
            <div className="flex min-h-screen items-center justify-center p-4 text-center sm:p-0">
                <div
                    className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
                    onClick={onClose}
                />

                <div className="relative transform overflow-hidden rounded-lg bg-white dark:bg-slate-900 text-right shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg border border-transparent dark:border-slate-800">
                    <div className="bg-white dark:bg-slate-900 px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="text-lg font-bold leading-6 text-gray-900 dark:text-white font-vazir">
                                {title}
                            </h3>
                            <button
                                onClick={onClose}
                                className="rounded-md text-gray-400 hover:text-gray-500 dark:hover:text-gray-300 focus:outline-none transition-colors"
                            >
                                <X className="h-6 w-6" />
                            </button>
                        </div>
                        <div className="mt-2 text-right">
                            {children}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};
