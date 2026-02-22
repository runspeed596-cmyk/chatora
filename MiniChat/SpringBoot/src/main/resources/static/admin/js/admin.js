// Admin Panel JavaScript Utilities

const API_URL = '';

// Check authentication
function checkAuth() {
    const token = localStorage.getItem('adminToken');
    const user = localStorage.getItem('adminUser');

    if (!token || !user) {
        window.location.href = 'index.html';
        return null;
    }

    return {
        token,
        user: JSON.parse(user)
    };
}

// Logout function
function logout() {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminUser');
    window.location.href = 'index.html';
}

// API request helper
async function apiRequest(endpoint, options = {}) {
    const auth = checkAuth();
    if (!auth) return;

    const defaultHeaders = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${auth.token}`
    };

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers
        }
    };

    try {
        const response = await fetch(API_URL + endpoint, config);
        const data = await response.json();

        if (response.status === 401 || response.status === 403) {
            logout();
            return null;
        }

        return data;
    } catch (error) {
        console.error('API Error:', error);
        showNotification('خطا در برقراری ارتباط با سرور', 'error');
        return null;
    }
}

// File upload helper
async function uploadFile(endpoint, file, additionalData = {}) {
    const auth = checkAuth();
    if (!auth) return;

    const formData = new FormData();
    formData.append('file', file);

    for (const [key, value] of Object.entries(additionalData)) {
        formData.append(key, value);
    }

    try {
        const response = await fetch(API_URL + endpoint, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${auth.token}`
            },
            body: formData
        });

        return await response.json();
    } catch (error) {
        console.error('Upload Error:', error);
        showNotification('خطا در آپلود فایل', 'error');
        return null;
    }
}

// Show notification
function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 24px;
        left: 50%;
        transform: translateX(-50%);
        background: ${type === 'success' ? 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' : 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'};
        color: white;
        padding: 16px 32px;
        border-radius: 12px;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
        z-index: 10000;
        animation: slideDown 0.3s ease-out;
    `;

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideUp 0.3s ease-out';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Add CSS animations for notifications
const style = document.createElement('style');
style.textContent = `
    @keyframes slideDown {
        from {
            opacity: 0;
            transform: translate(-50%, -20px);
        }
        to {
            opacity: 1;
            transform: translate(-50%, 0);
        }
    }
    
    @keyframes slideUp {
        from {
            opacity: 1;
            transform: translate(-50%, 0);
        }
        to {
            opacity: 0;
            transform: translate(-50%, -20px);
        }
    }
`;
document.head.appendChild(style);

// Modal helper
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
    }
}

// Format price (Toman)
function formatPrice(price) {
    return new Intl.NumberFormat('fa-IR').format(price) + ' تومان';
}

// Format date
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fa-IR').format(date);
}

// Confirm dialog
function confirmAction(message) {
    return confirm(message);
}

// Set button loading state
function setLoading(button, isLoading) {
    if (isLoading) {
        button.disabled = true;
        button.dataset.originalText = button.textContent;
        button.textContent = 'در حال پردازش...';
    } else {
        button.disabled = false;
        button.textContent = button.dataset.originalText || button.textContent;
    }
}

// Debounce function
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Image preview helper
function previewImage(input, previewContainer) {
    if (input.files && input.files.length > 0) {
        Array.from(input.files).forEach(file => {
            const reader = new FileReader();
            reader.onload = (e) => {
                const div = document.createElement('div');
                div.className = 'image-preview-item';
                div.innerHTML = `
                    <img src="${e.target.result}" alt="Preview">
                    <button type="button" class="remove-image" onclick="this.parentElement.remove()">×</button>
                `;
                previewContainer.appendChild(div);
            };
            reader.readAsDataURL(file);
        });
    }
}

// Validation helpers
function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

function validatePhone(phone) {
    const re = /^09\d{9}$/;
    return re.test(phone);
}

function validateRequired(value) {
    return value && value.trim().length > 0;
}

// Loading state
function setLoading(element, loading) {
    if (loading) {
        element.disabled = true;
        element.dataset.originalText = element.textContent;
        element.textContent = 'در حال پردازش...';
    } else {
        element.disabled = false;
        element.textContent = element.dataset.originalText;
    }
}

// Table helper
function renderTable(data, columns, tbody) {
    tbody.innerHTML = '';

    if (!data || data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="' + columns.length + '" class="text-center">داده‌ای یافت نشد</td></tr>';
        return;
    }

    data.forEach(item => {
        const row = document.createElement('tr');
        columns.forEach(col => {
            const cell = document.createElement('td');
            cell.innerHTML = col.render ? col.render(item) : item[col.field];
            row.appendChild(cell);
        });
        tbody.appendChild(row);
    });
}

// Pagination helper
function renderPagination(totalItems, pageSize, currentPage, container, onPageChange) {
    const totalPages = Math.ceil(totalItems / pageSize);
    container.innerHTML = '';

    // Previous button
    const prevBtn = document.createElement('button');
    prevBtn.textContent = 'قبلی';
    prevBtn.disabled = currentPage === 0;
    prevBtn.onclick = () => onPageChange(currentPage - 1);
    container.appendChild(prevBtn);

    // Page buttons
    for (let i = 0; i < totalPages; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.textContent = i + 1;
        pageBtn.className = i === currentPage ? 'active' : '';
        pageBtn.onclick = () => onPageChange(i);
        container.appendChild(pageBtn);
    }

    // Next button
    const nextBtn = document.createElement('button');
    nextBtn.textContent = 'بعدی';
    nextBtn.disabled = currentPage === totalPages - 1;
    nextBtn.onclick = () => onPageChange(currentPage + 1);
    container.appendChild(nextBtn);
}

// Export
window.adminUtils = {
    checkAuth,
    logout,
    apiRequest,
    uploadFile,
    showNotification,
    openModal,
    closeModal,
    formatPrice,
    formatDate,
    confirmAction,
    debounce,
    previewImage,
    validateEmail,
    validatePhone,
    validateRequired,
    setLoading,
    renderTable,
    renderPagination
};
