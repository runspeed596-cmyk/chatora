import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { SubscriptionCard } from '../components/subscriptions/SubscriptionCard';
import { subscriptionService } from '../services/subscriptionService';
import { Modal } from '../components/ui/Modal';
import type { SubscriptionPlan } from '../types/subscription';

export const Subscriptions: React.FC = () => {
    const queryClient = useQueryClient();
    const [selectedPlan, setSelectedPlan] = useState<SubscriptionPlan | null>(null);
    const [price, setPrice] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);

    const { data: plans, isLoading } = useQuery({
        queryKey: ['plans'],
        queryFn: subscriptionService.getPlans,
    });

    const updateMutation = useMutation({
        mutationFn: subscriptionService.updatePlan,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['plans'] });
            closeModal();
        },
    });

    const handleEdit = (plan: SubscriptionPlan) => {
        setSelectedPlan(plan);
        setPrice(plan.price.toString());
        setIsModalOpen(true);
    };

    const closeModal = () => {
        setIsModalOpen(false);
        setSelectedPlan(null);
        setPrice('');
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (selectedPlan && price) {
            updateMutation.mutate({
                id: selectedPlan.id,
                price: Number(price)
            });
        }
    };

    if (isLoading) return <div className="p-8 text-center text-gray-500">در حال بارگذاری...</div>;

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white font-vazir">مدیریت اشتراک‌ها</h2>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {plans?.map((plan) => (
                    <SubscriptionCard key={plan.id} plan={plan} onEdit={handleEdit} />
                ))}
            </div>

            <Modal
                isOpen={isModalOpen}
                onClose={closeModal}
                title={`ویرایش تعرفه ${selectedPlan?.name}`}
            >
                <form onSubmit={handleSubmit} className="mt-4 space-y-4 font-vazir">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            قیمت جدید (دلار)
                        </label>
                        <input
                            type="number"
                            value={price}
                            onChange={(e) => setPrice(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none font-vazir bg-white dark:bg-slate-800 text-gray-900 dark:text-white"
                            placeholder="قیمت را وارد کنید"
                            min="0"
                            step="0.01"
                            required
                        />
                    </div>

                    <div className="flex gap-3 justify-end mt-6">
                        <button
                            type="button"
                            onClick={closeModal}
                            className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-slate-700 rounded-lg hover:bg-gray-200 dark:hover:bg-slate-600 transition-colors font-vazir"
                        >
                            انصراف
                        </button>
                        <button
                            type="submit"
                            disabled={updateMutation.isPending}
                            className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50 font-vazir"
                        >
                            {updateMutation.isPending ? 'در حال ذخیره...' : 'ذخیره تغییرات'}
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
};
