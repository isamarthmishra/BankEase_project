/**
 * BankEase REST API Client
 * Manages JWT tokens, authenticated requests, and endpoint invocations
 */

const API = (() => {
    const BASE_URL = window.location.origin;

    const getToken = () => localStorage.getItem('bankease_token');
    const setToken = (token) => localStorage.setItem('bankease_token', token);
    const removeToken = () => localStorage.removeItem('bankease_token');

    const getUser = () => {
        const u = localStorage.getItem('bankease_user');
        return u ? JSON.parse(u) : null;
    };
    const setUser = (user) => localStorage.setItem('bankease_user', JSON.stringify(user));
    const removeUser = () => localStorage.removeItem('bankease_user');

    async function request(endpoint, options = {}) {
        const url = `${BASE_URL}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);
            const contentType = response.headers.get('content-type');
            let data = null;
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                data = await response.text();
            }

            if (!response.ok) {
                if (response.status === 401 && !endpoint.includes('/api/auth/')) {
                    // Session expired or invalid token
                    removeToken();
                    removeUser();
                    window.dispatchEvent(new Event('auth:unauthorized'));
                }
                const errorMessage = (data && data.message) || data || `Request failed with status ${response.status}`;
                throw new Error(errorMessage);
            }

            return data;
        } catch (error) {
            console.error(`API Error on ${endpoint}:`, error);
            throw error;
        }
    }

    return {
        getToken,
        getUser,
        isAuthenticated: () => !!getToken(),

        // Auth
        login: async (email, password) => {
            const data = await request('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password })
            });
            setToken(data.token);
            setUser({ id: data.id, name: data.name, email: data.email });
            return data;
        },

        register: async (name, email, password, phone) => {
            const data = await request('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({ name, email, password, phone })
            });
            setToken(data.token);
            setUser({ id: data.id, name: data.name, email: data.email });
            return data;
        },

        logout: () => {
            removeToken();
            removeUser();
        },

        // Users / Profile
        getMe: () => request('/api/users/me'),
        updateProfile: (id, userData) => request(`/api/users/${id}`, {
            method: 'PUT',
            body: JSON.stringify(userData)
        }),


        // Accounts
        getAccounts: () => request('/api/accounts'),
        getAccount: (accountNumber) => request(`/api/accounts/${accountNumber}`),
        getBalance: (accountNumber) => request(`/api/accounts/${accountNumber}/balance`),
        createAccount: (accountType, initialDeposit, currency = 'INR') => request('/api/accounts', {
            method: 'POST',
            body: JSON.stringify({ accountType, initialDeposit, currency })
        }),
        deposit: (accountNumber, amount, description = 'Online Deposit') => request(`/api/accounts/${accountNumber}/deposit`, {
            method: 'POST',
            body: JSON.stringify({ amount, description })
        }),

        // Transfers
        transfer: (sourceAccountNumber, targetAccountNumber, amount, description, idempotencyKey) => request('/api/transfers', {
            method: 'POST',
            body: JSON.stringify({ sourceAccountNumber, targetAccountNumber, amount, description, idempotencyKey })
        }),

        // Transactions
        getAllTransactions: () => request('/api/transactions'),
        getAccountTransactions: (accountNumber) => request(`/api/transactions/account/${accountNumber}`),
        getTransactionReceipt: (referenceNumber) => request(`/api/transactions/receipt/${referenceNumber}`),

        // Beneficiaries
        getBeneficiaries: () => request('/api/beneficiaries'),
        addBeneficiary: (name, accountNumber, bankName = 'BankEase', ifscCode = 'BANKEASE001', nickname = '') => request('/api/beneficiaries', {
            method: 'POST',
            body: JSON.stringify({ name, accountNumber, bankName, ifscCode, nickname })
        }),
        deleteBeneficiary: (id) => request(`/api/beneficiaries/${id}`, {
            method: 'DELETE'
        }),

        // Bills
        getBills: (status = '') => request(status ? `/api/bills?status=${status}` : '/api/bills'),
        createBill: (billerName, category, consumerNumber, amount, dueDate) => request('/api/bills', {
            method: 'POST',
            body: JSON.stringify({ billerName, category, consumerNumber, amount, dueDate })
        }),
        payBill: (billId, accountNumber) => request(`/api/bills/${billId}/pay`, {
            method: 'POST',
            body: JSON.stringify({ accountNumber })
        }),

        // Loans
        getLoans: () => request('/api/loans'),
        applyLoan: (accountNumber, loanType, principalAmount, termMonths) => request('/api/loans/apply', {
            method: 'POST',
            body: JSON.stringify({ accountNumber, loanType, principalAmount, termMonths })
        }),
        repayLoan: (loanId, amount) => request(`/api/loans/${loanId}/repay`, {
            method: 'POST',
            body: JSON.stringify({ amount })
        }),

        // Investments
        getInvestments: () => request('/api/investments'),
        createInvestment: (accountNumber, investmentType, amount, termMonths) => request('/api/investments', {
            method: 'POST',
            body: JSON.stringify({ accountNumber, investmentType, amount, termMonths })
        }),
        withdrawInvestment: (investmentId) => request(`/api/investments/${investmentId}/withdraw`, {
            method: 'POST'
        })
    };
})();
