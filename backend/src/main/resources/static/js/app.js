/**
 * BankEase Application Controller
 * Handles SPA navigation, real-time UI state, form actions, and data rendering
 */

const App = (() => {
    let currentAccounts = [];
    let currentBeneficiaries = [];
    let currentTransactions = [];
    let currentBills = [];
    let currentLoans = [];
    let currentInvestments = [];

    // Helper: Formatter
    const formatINR = (val) => {
        const num = Number(val) || 0;
        return '₹' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    };

    const formatDate = (isoStr) => {
        if (!isoStr) return '-';
        const d = new Date(isoStr);
        return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    };

    // Toast Notifications
    const showToast = (message, type = 'success') => {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
        toast.innerHTML = `<span>${icon}</span><div>${message}</div>`;
        container.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    };

    // SPA Navigation
    const navigate = (viewName) => {
        const views = ['auth', 'dashboard', 'accounts', 'transfers', 'beneficiaries', 'bills', 'loans', 'investments', 'transactions', 'docs'];
        views.forEach(v => {
            const el = document.getElementById(`view-${v}`);
            if (el) el.style.display = (v === viewName) ? 'block' : 'none';
            const navBtn = document.getElementById(`nav-btn-${v}`);
            if (navBtn) {
                if (v === viewName) navBtn.classList.add('active');
                else navBtn.classList.remove('active');
            }
        });

        // Trigger view-specific data refresh
        if (API.isAuthenticated()) {
            if (viewName === 'dashboard') loadDashboard();
            if (viewName === 'accounts') loadAccounts();
            if (viewName === 'transfers') loadTransfersView();
            if (viewName === 'beneficiaries') loadBeneficiaries();
            if (viewName === 'bills') loadBills();
            if (viewName === 'loans') loadLoans();
            if (viewName === 'investments') loadInvestments();
            if (viewName === 'transactions') loadTransactions();
            if (viewName === 'docs') renderApiDocs();
        }
    };

    const togglePasswordVisibility = (inputId, btn) => {
        const input = document.getElementById(inputId);
        if (!input) return;
        if (input.type === 'password') {
            input.type = 'text';
            btn.textContent = '🔒';
            btn.setAttribute('title', 'Hide Password');
        } else {
            input.type = 'password';
            btn.textContent = '👁️';
            btn.setAttribute('title', 'Show Password');
        }
    };

    // Initialize Auth UI
    const updateAuthUI = (cachedUser) => {
        const user = cachedUser || API.getUser();
        const isAuth = API.isAuthenticated() && user;
        const navMenu = document.getElementById('nav-menu');
        const userBadge = document.getElementById('user-header-badge');
        const authBtn = document.getElementById('auth-action-btn');

        if (isAuth) {
            navMenu.style.display = 'flex';
            userBadge.style.display = 'flex';
            document.getElementById('user-display-name').textContent = user.name || user.email;
            document.getElementById('user-avatar-text').textContent = (user.name || user.email).charAt(0).toUpperCase();
            authBtn.textContent = 'Logout';
            authBtn.onclick = () => onLogout();
            navigate('dashboard');
            // Refresh user details in background to sync profile
            API.getMe().then(freshUser => {
                if (freshUser && freshUser.name) {
                    const u = { id: freshUser.id, name: freshUser.name, email: freshUser.email };
                    localStorage.setItem('bankease_user', JSON.stringify(u));
                    const nameEl = document.getElementById('user-display-name');
                    const avatarEl = document.getElementById('user-avatar-text');
                    if (nameEl) nameEl.textContent = u.name;
                    if (avatarEl) avatarEl.textContent = u.name.charAt(0).toUpperCase();
                }
            }).catch(() => {});
        } else {
            navMenu.style.display = 'none';
            userBadge.style.display = 'none';
            authBtn.textContent = 'Login';
            authBtn.onclick = () => navigate('auth');
            navigate('auth');
        }
    };

    const handleAuthButton = () => {
        if (API.isAuthenticated()) onLogout();
        else navigate('auth');
    };

    const switchAuthTab = (tab) => {
        const loginForm = document.getElementById('form-login');
        const regForm = document.getElementById('form-register');
        const tabLogin = document.getElementById('tab-login');
        const tabReg = document.getElementById('tab-register');
        const title = document.getElementById('auth-title');
        const subtitle = document.getElementById('auth-subtitle');

        if (tab === 'login') {
            loginForm.style.display = 'block';
            regForm.style.display = 'none';
            tabLogin.style.background = 'var(--bg-card)';
            tabLogin.style.color = 'var(--text-primary)';
            tabReg.style.background = 'transparent';
            tabReg.style.color = 'var(--text-secondary)';
            title.textContent = 'Welcome to BankEase';
            subtitle.textContent = 'Enter your credentials to access your secure portal';
        } else {
            loginForm.style.display = 'none';
            regForm.style.display = 'block';
            tabReg.style.background = 'var(--bg-card)';
            tabReg.style.color = 'var(--text-primary)';
            tabLogin.style.background = 'transparent';
            tabLogin.style.color = 'var(--text-secondary)';
            title.textContent = 'Join BankEase Today';
            subtitle.textContent = 'Complete registration in less than 30 seconds';
        }
    };

    const onLogin = async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value.trim();
        const password = document.getElementById('login-password').value;
        const btn = document.getElementById('btn-submit-login');

        try {
            btn.disabled = true;
            btn.textContent = 'Authenticating...';
            await API.login(email, password);
            showToast(`Welcome back, ${email}!`, 'success');
            updateAuthUI();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Sign In to Portal';
        }
    };

    const onRegister = async (e) => {
        e.preventDefault();
        const name = document.getElementById('reg-name').value.trim();
        const email = document.getElementById('reg-email').value.trim();
        const password = document.getElementById('reg-password').value;
        const phone = document.getElementById('reg-phone').value.trim();
        const btn = document.getElementById('btn-submit-register');

        try {
            btn.disabled = true;
            btn.textContent = 'Creating Account...';
            await API.register(name, email, password, phone);
            showToast('Registration successful! Opening primary account...', 'success');

            // Automatically open a primary Savings Account with 5000 demo initial deposit
            try {
                await API.createAccount('SAVINGS', 5000.00, 'INR');
            } catch (ignored) {}

            updateAuthUI();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Create Banking Profile';
        }
    };

    const onLogout = () => {
        API.logout();
        showToast('Logged out successfully', 'info');
        updateAuthUI();
    };

    const fillDemoCredentials = () => {
        const emailInput = document.getElementById('login-email');
        const passInput = document.getElementById('login-password');
        if (emailInput) emailInput.value = 'live.demo@bankease.com';
        if (passInput) passInput.value = 'SecurePassword@123';
        switchAuthTab('login');
        showToast('Demo credentials filled into login form', 'info');
    };

    // -------------------------------------------------------------
    // Customer Profile & Security
    // -------------------------------------------------------------
    const openProfileModal = async () => {
        try {
            const user = await API.getMe();
            document.getElementById('profile-user-id').value = user.id || '';
            document.getElementById('profile-name').value = user.name || '';
            document.getElementById('profile-email').value = user.email || '';
            document.getElementById('profile-phone').value = user.phone || '';
            document.getElementById('profile-password').value = '';
            document.getElementById('profile-confirm-password').value = '';
            document.getElementById('profile-created-date').textContent = user.createdAt ? formatDate(user.createdAt) : 'Verified Customer';
            const avatarLarge = document.getElementById('profile-avatar-large');
            if (avatarLarge && user.name) {
                avatarLarge.textContent = user.name.charAt(0).toUpperCase();
            }
            document.getElementById('modal-profile').classList.add('open');
        } catch (err) {
            showToast('Failed to load profile: ' + err.message, 'error');
        }
    };

    const onUpdateProfile = async (e) => {
        e.preventDefault();
        const id = document.getElementById('profile-user-id').value;
        const name = document.getElementById('profile-name').value.trim();
        const email = document.getElementById('profile-email').value.trim();
        const phone = document.getElementById('profile-phone').value.trim();
        const password = document.getElementById('profile-password').value;
        const confirmPassword = document.getElementById('profile-confirm-password').value;

        if (password) {
            if (password.length < 6) {
                showToast('New password must be at least 6 characters.', 'error');
                return;
            }
            if (password !== confirmPassword) {
                showToast('New password and confirmation do not match.', 'error');
                return;
            }
        }

        const payload = { name, email, phone };
        if (password) {
            payload.password = password;
        }

        try {
            const updated = await API.updateProfile(id, payload);
            showToast('Profile updated successfully!', 'success');
            const current = API.getUser() || {};
            const newUser = { ...current, name: updated.name, email: updated.email };
            localStorage.setItem('bankease_user', JSON.stringify(newUser));
            updateAuthUI(newUser);
            closeModals();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    // -------------------------------------------------------------
    // Dashboard & Account Management
    // -------------------------------------------------------------
    const loadDashboard = async () => {
        const user = API.getUser();
        if (user) {
            document.getElementById('dash-user-name').textContent = user.name || user.email;
        }

        try {
            const [accounts, txns, loans, investments] = await Promise.all([
                API.getAccounts(),
                API.getAllTransactions(),
                API.getLoans().catch(() => []),
                API.getInvestments().catch(() => [])
            ]);

            currentAccounts = accounts;
            currentTransactions = txns;
            currentLoans = loans;
            currentInvestments = investments;

            // Compute totals
            const totalLiquid = accounts.reduce((sum, a) => sum + (Number(a.balance) || 0), 0);
            const totalInvested = investments
                .filter(i => i.status === 'ACTIVE')
                .reduce((sum, i) => sum + (Number(i.investedAmount) || 0), 0);
            const totalLoansRemaining = loans
                .filter(l => l.status === 'ACTIVE')
                .reduce((sum, l) => sum + (Number(l.remainingAmount) || 0), 0);

            document.getElementById('dash-total-networth').textContent = formatINR(totalLiquid + totalInvested);
            document.getElementById('dash-active-accounts').textContent = accounts.length;
            document.getElementById('dash-total-investments').textContent = formatINR(totalInvested);
            document.getElementById('dash-total-loans').textContent = formatINR(totalLoansRemaining);

            // Render Accounts
            renderAccountCards(accounts, 'dashboard-accounts-container');

            // Render Recent Transactions
            renderRecentTransactions(txns.slice(0, 5));
        } catch (err) {
            showToast('Failed to load dashboard: ' + err.message, 'error');
        }
    };

    const renderAccountCards = (accounts, containerId) => {
        const container = document.getElementById(containerId);
        if (!container) return;

        if (!accounts || accounts.length === 0) {
            container.innerHTML = `
                <div class="glass-card" style="grid-column: 1 / -1; text-align: center; padding: 3rem;">
                    <p style="color: var(--text-secondary); margin-bottom: 1.5rem;">You do not have any active bank accounts yet.</p>
                    <button class="btn btn-primary" onclick="App.openCreateAccountModal()">+ Open Your First Account</button>
                </div>
            `;
            return;
        }

        container.innerHTML = accounts.map(acc => {
            const typeClass = acc.accountType.toLowerCase();
            return `
                <div class="bank-card ${typeClass}">
                    <div class="bank-card-header">
                        <div class="bank-chip"></div>
                        <span class="bank-card-type">${acc.accountType}</span>
                    </div>
                    <div class="bank-card-number">${formatCardNumber(acc.accountNumber)}</div>
                    <div class="bank-card-footer">
                        <div>
                            <div class="bank-card-balance-label">Available Balance</div>
                            <div class="bank-card-balance-val">${formatINR(acc.balance)}</div>
                        </div>
                        <button class="btn btn-sm btn-primary" onclick="App.openDepositModal('${acc.accountNumber}')">
                            + Deposit
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    };

    const formatCardNumber = (numStr) => {
        if (!numStr) return '•••• •••• ••••';
        const str = String(numStr);
        return str.replace(/(\d{4})(?=\d)/g, '$1 ');
    };

    const renderRecentTransactions = (txns) => {
        const tbody = document.getElementById('dash-recent-transactions');
        if (!tbody) return;

        if (!txns || txns.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">No transaction activity found.</td></tr>';
            return;
        }

        tbody.innerHTML = txns.map(t => {
            const isCredit = t.transactionType === 'DEPOSIT' || t.description?.includes('Disbursement');
            const amtColor = isCredit ? 'var(--accent-success)' : 'var(--text-primary)';
            const prefix = isCredit ? '+' : '-';

            return `
                <tr style="cursor: pointer;" onclick="App.viewReceipt('${t.referenceNumber}')">
                    <td>
                        <strong style="color: var(--primary);">${t.referenceNumber}</strong>
                        <div style="font-size: 0.75rem; color: var(--text-muted);">${formatDate(t.timestamp)}</div>
                    </td>
                    <td><span class="badge badge-info">${t.transactionType}</span></td>
                    <td>
                        <div style="font-size: 0.85rem;">${t.sourceAccountNumber || 'External / System'} &rarr; ${t.targetAccountNumber || 'BankEase'}</div>
                        <div style="font-size: 0.75rem; color: var(--text-secondary);">${t.description || '-'}</div>
                    </td>
                    <td style="color: ${amtColor}; font-weight: 700;">${prefix}${formatINR(t.amount)}</td>
                    <td><span class="badge badge-success">${t.status}</span></td>
                </tr>
            `;
        }).join('');
    };

    const loadAccounts = async () => {
        try {
            const accounts = await API.getAccounts();
            currentAccounts = accounts;
            renderAccountCards(accounts, 'accounts-full-list');
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    // Open Create Account Modal
    const openCreateAccountModal = () => {
        document.getElementById('modal-create-account').classList.add('open');
    };

    const onCreateAccount = async (e) => {
        e.preventDefault();
        const type = document.getElementById('create-acc-type').value;
        const initial = parseFloat(document.getElementById('create-acc-deposit').value) || 0;

        try {
            await API.createAccount(type, initial, 'INR');
            showToast(`New ${type} Account created successfully!`, 'success');
            closeModals();
            loadDashboard();
            loadAccounts();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    // Deposit Modal
    const openDepositModal = (accountNumber) => {
        document.getElementById('deposit-acc-number').value = accountNumber;
        document.getElementById('deposit-acc-display').value = accountNumber;
        document.getElementById('deposit-amount').value = '';
        document.getElementById('modal-deposit').classList.add('open');
    };

    const onDeposit = async (e) => {
        e.preventDefault();
        const accNum = document.getElementById('deposit-acc-number').value;
        const amount = parseFloat(document.getElementById('deposit-amount').value);
        const desc = document.getElementById('deposit-desc').value;

        try {
            const receipt = await API.deposit(accNum, amount, desc);
            showToast(`Successfully deposited ${formatINR(amount)}!`, 'success');
            closeModals();
            loadDashboard();
            displayReceiptModal(receipt);
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    // -------------------------------------------------------------
    // Fund Transfers
    // -------------------------------------------------------------
    const loadTransfersView = async () => {
        try {
            const [accounts, beneficiaries] = await Promise.all([
                API.getAccounts(),
                API.getBeneficiaries()
            ]);
            currentAccounts = accounts;
            currentBeneficiaries = beneficiaries;

            const sourceSelect = document.getElementById('transfer-source-acc');
            sourceSelect.innerHTML = accounts.map(a =>
                `<option value="${a.accountNumber}" data-balance="${a.balance}">${a.accountNumber} (${a.accountType}) — ${formatINR(a.balance)}</option>`
            ).join('');

            onTransferSourceChange();

            const benSelect = document.getElementById('transfer-beneficiary-select');
            benSelect.innerHTML = '<option value="">-- Select a saved beneficiary or enter manually below --</option>' +
                beneficiaries.map(b => `<option value="${b.accountNumber}">${b.name} (${b.bankName} - ${b.accountNumber})</option>`).join('');

        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const onTransferSourceChange = () => {
        const sourceSelect = document.getElementById('transfer-source-acc');
        const selectedOpt = sourceSelect.options[sourceSelect.selectedIndex];
        if (selectedOpt) {
            const bal = selectedOpt.getAttribute('data-balance');
            document.getElementById('transfer-source-balance-hint').textContent = `Available Balance: ${formatINR(bal)}`;
        }
    };

    const onBeneficiarySelect = (selectEl) => {
        const val = selectEl.value;
        if (val) {
            document.getElementById('transfer-target-acc').value = val;
        }
    };

    const setTransferAmount = (amt) => {
        document.getElementById('transfer-amount').value = amt;
    };

    const onExecuteTransfer = async (e) => {
        e.preventDefault();
        const sourceAcc = document.getElementById('transfer-source-acc').value;
        const targetAcc = document.getElementById('transfer-target-acc').value.trim();
        const amount = parseFloat(document.getElementById('transfer-amount').value);
        const desc = document.getElementById('transfer-description').value.trim();

        // Generate client idempotency key to prevent double charging
        const idempotencyKey = 'IDEMP-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
        const btn = document.getElementById('btn-submit-transfer');

        try {
            btn.disabled = true;
            btn.textContent = 'Executing Secure Transfer...';

            const receipt = await API.transfer(sourceAcc, targetAcc, amount, desc, idempotencyKey);
            showToast('Transfer completed successfully!', 'success');
            displayReceiptModal(receipt);
            document.getElementById('transfer-amount').value = '';
            document.getElementById('transfer-description').value = '';
            loadTransfersView();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = '🔒 Confirm & Transfer Funds';
        }
    };

    // -------------------------------------------------------------
    // Receipts Modal
    // -------------------------------------------------------------
    const displayReceiptModal = (receipt) => {
        document.getElementById('receipt-ref').textContent = receipt.referenceNumber;
        document.getElementById('receipt-date').textContent = formatDate(receipt.timestamp);
        document.getElementById('receipt-type').textContent = receipt.transactionType;
        document.getElementById('receipt-status').innerHTML = `<span class="badge badge-success">${receipt.status}</span>`;
        document.getElementById('receipt-source').textContent = receipt.sourceAccountNumber || 'External Deposit';
        document.getElementById('receipt-target').textContent = receipt.targetAccountNumber || 'BankEase Account';
        document.getElementById('receipt-amount').textContent = formatINR(receipt.amount);
        document.getElementById('receipt-balance-after').textContent = receipt.balanceAfter != null ? formatINR(receipt.balanceAfter) : '-';
        document.getElementById('receipt-desc').textContent = receipt.description || '-';

        document.getElementById('modal-receipt').classList.add('open');
    };

    const viewReceipt = async (referenceNumber) => {
        try {
            const receipt = await API.getTransactionReceipt(referenceNumber);
            displayReceiptModal(receipt);
        } catch (err) {
            showToast('Receipt error: ' + err.message, 'error');
        }
    };

    const copyReceiptRef = () => {
        const ref = document.getElementById('receipt-ref').textContent;
        navigator.clipboard.writeText(ref);
        showToast(`Reference ${ref} copied to clipboard!`, 'info');
    };

    // -------------------------------------------------------------
    // Beneficiary Management
    // -------------------------------------------------------------
    const loadBeneficiaries = async () => {
        try {
            const list = await API.getBeneficiaries();
            currentBeneficiaries = list;
            const container = document.getElementById('beneficiaries-list-container');
            if (!list || list.length === 0) {
                container.innerHTML = '<p style="color: var(--text-muted); text-align: center; padding: 2rem;">No saved beneficiaries yet.</p>';
                return;
            }

            container.innerHTML = list.map(b => `
                <div style="display: flex; justify-content: space-between; align-items: center; padding: 1rem; background: rgba(255,255,255,0.03); border: 1px solid var(--border-glass); border-radius: var(--radius-md); margin-bottom: 0.75rem;">
                    <div>
                        <div style="font-weight: 700; font-size: 1.05rem;">${b.name} ${b.nickname ? `<span style="font-size: 0.8rem; color: var(--text-secondary);">(${b.nickname})</span>` : ''}</div>
                        <div style="font-size: 0.85rem; color: var(--text-muted); font-family: monospace;">${b.accountNumber} &bull; ${b.bankName} (${b.ifscCode})</div>
                    </div>
                    <div style="display: flex; gap: 0.5rem;">
                        <button class="btn btn-primary btn-sm" onclick="App.transferToBeneficiary('${b.accountNumber}')">Transfer</button>
                        <button class="btn btn-danger btn-sm" onclick="App.onDeleteBeneficiary(${b.id})">Delete</button>
                    </div>
                </div>
            `).join('');
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const transferToBeneficiary = (accNumber) => {
        navigate('transfers');
        document.getElementById('transfer-target-acc').value = accNumber;
    };

    const onAddBeneficiary = async (e) => {
        e.preventDefault();
        const name = document.getElementById('ben-name').value.trim();
        const acc = document.getElementById('ben-acc').value.trim();
        const bank = document.getElementById('ben-bank').value.trim();
        const ifsc = document.getElementById('ben-ifsc').value.trim();
        const nick = document.getElementById('ben-nickname').value.trim();

        try {
            await API.addBeneficiary(name, acc, bank, ifsc, nick);
            showToast('Beneficiary added successfully!', 'success');
            document.getElementById('ben-name').value = '';
            document.getElementById('ben-acc').value = '';
            document.getElementById('ben-nickname').value = '';
            loadBeneficiaries();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const onDeleteBeneficiary = async (id) => {
        if (!confirm('Are you sure you want to remove this beneficiary?')) return;
        try {
            await API.deleteBeneficiary(id);
            showToast('Beneficiary removed', 'info');
            loadBeneficiaries();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    // -------------------------------------------------------------
    // Utility & Bill Payments
    // -------------------------------------------------------------
    let currentBillFilter = 'ALL';

    const filterBills = (status) => {
        currentBillFilter = status;
        ['ALL', 'PENDING', 'PAID'].forEach(s => {
            const btn = document.getElementById(`bill-filter-${s}`);
            if (btn) {
                if (s === status) btn.classList.add('active');
                else btn.classList.remove('active');
            }
        });
        renderFilteredBills();
    };

    const renderFilteredBills = () => {
        const tbody = document.getElementById('bills-table-body');
        if (!tbody) return;

        let filtered = currentBills || [];
        if (currentBillFilter !== 'ALL') {
            filtered = (currentBills || []).filter(b => b.status === currentBillFilter);
        }

        if (!filtered || filtered.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 2rem;">No ${currentBillFilter === 'ALL' ? '' : currentBillFilter.toLowerCase()} bills found.</td></tr>`;
            return;
        }

        tbody.innerHTML = filtered.map(b => {
            const isPaid = b.status === 'PAID';
            const statusBadge = isPaid
                ? `<span class="badge badge-success">PAID</span>`
                : `<span class="badge badge-pending">PENDING</span>`;

            const actionBtn = isPaid
                ? `<button class="btn btn-secondary btn-sm" onclick="App.viewReceipt('${b.transactionReference}')">View Receipt</button>`
                : `<button class="btn btn-primary btn-sm" onclick="App.openPayBillModal(${b.id}, '${b.billerName}', ${b.amount})">Pay Now</button>`;

            return `
                <tr>
                    <td><strong>${b.billerName}</strong></td>
                    <td><span class="badge badge-info">${b.category}</span></td>
                    <td style="font-family: monospace;">${b.consumerNumber}</td>
                    <td style="font-weight: 700; color: var(--text-primary);">${formatINR(b.amount)}</td>
                    <td>${statusBadge}</td>
                    <td>${actionBtn}</td>
                </tr>
            `;
        }).join('');
    };

    const loadBills = async () => {
        try {
            const bills = await API.getBills();
            currentBills = bills;
            renderFilteredBills();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const onCreateBill = async (e) => {
        e.preventDefault();
        const biller = document.getElementById('bill-name').value.trim();
        const category = document.getElementById('bill-category').value;
        const consumerNo = document.getElementById('bill-consumer-no').value.trim();
        const amount = parseFloat(document.getElementById('bill-amount').value);
        const dueDate = document.getElementById('bill-due-date').value || null;

        try {
            await API.createBill(biller, category, consumerNo, amount, dueDate);
            showToast('Bill registered successfully!', 'success');
            document.getElementById('bill-name').value = '';
            document.getElementById('bill-consumer-no').value = '';
            document.getElementById('bill-amount').value = '';
            loadBills();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const openPayBillModal = async (billId, billerName, amount) => {
        document.getElementById('paybill-bill-id').value = billId;
        document.getElementById('paybill-biller').value = billerName;
        document.getElementById('paybill-amount').value = formatINR(amount);

        // Populate accounts
        const accounts = await API.getAccounts();
        const select = document.getElementById('paybill-account-select');
        select.innerHTML = accounts.map(a =>
            `<option value="${a.accountNumber}">${a.accountNumber} (${a.accountType}) — Balance: ${formatINR(a.balance)}</option>`
        ).join('');

        document.getElementById('modal-pay-bill').classList.add('open');
    };

    const onConfirmPayBill = async (e) => {
        e.preventDefault();
        const billId = document.getElementById('paybill-bill-id').value;
        const accNum = document.getElementById('paybill-account-select').value;

        try {
            const bill = await API.payBill(billId, accNum);
            showToast(`Bill paid successfully from account ${accNum}!`, 'success');
            closeModals();
            loadBills();
            loadDashboard();
            if (bill.transactionReference) {
                viewReceipt(bill.transactionReference);
            }
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    // -------------------------------------------------------------
    // Loan Management
    // -------------------------------------------------------------
    const loadLoans = async () => {
        try {
            const [accounts, loans] = await Promise.all([
                API.getAccounts(),
                API.getLoans()
            ]);
            currentAccounts = accounts;
            currentLoans = loans;

            const accSelect = document.getElementById('loan-account-select');
            accSelect.innerHTML = accounts.map(a =>
                `<option value="${a.accountNumber}">${a.accountNumber} (${a.accountType})</option>`
            ).join('');

            recalculateLoanEMI();

            const container = document.getElementById('loans-list-container');
            if (!loans || loans.length === 0) {
                container.innerHTML = '<p style="color: var(--text-muted); text-align: center; padding: 2rem;">No loans taken yet.</p>';
                return;
            }

            container.innerHTML = loans.map(l => {
                const isRepaid = l.status === 'REPAID';
                const statusBadge = isRepaid
                    ? `<span class="badge badge-success">REPAID</span>`
                    : `<span class="badge badge-pending">ACTIVE</span>`;

                return `
                    <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-glass); border-radius: var(--radius-md); padding: 1.25rem; margin-bottom: 1rem;">
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                            <div>
                                <span style="font-weight: 700; font-size: 1.1rem;">${l.loanType} LOAN</span>
                                <div style="font-family: monospace; font-size: 0.8rem; color: var(--primary);">${l.loanNumber}</div>
                            </div>
                            ${statusBadge}
                        </div>
                        <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; font-size: 0.85rem; margin-bottom: 1rem;">
                            <div>
                                <div style="color: var(--text-muted);">Principal</div>
                                <div style="font-weight: 700;">${formatINR(l.principalAmount)}</div>
                            </div>
                            <div>
                                <div style="color: var(--text-muted);">Monthly EMI</div>
                                <div style="font-weight: 700; color: var(--primary);">${formatINR(l.monthlyInstallment)}</div>
                            </div>
                            <div>
                                <div style="color: var(--text-muted);">Remaining Due</div>
                                <div style="font-weight: 700; color: var(--accent-danger);">${formatINR(l.remainingAmount)}</div>
                            </div>
                        </div>
                        ${!isRepaid ? `
                            <button class="btn btn-primary btn-sm" style="width: 100%;" onclick="App.openRepayLoanModal(${l.id}, '${l.loanNumber}', ${l.remainingAmount}, ${l.monthlyInstallment})">
                                Pay Installment (${formatINR(l.monthlyInstallment)})
                            </button>
                        ` : ''}
                    </div>
                `;
            }).join('');
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const recalculateLoanEMI = () => {
        const type = document.getElementById('loan-type-select').value;
        const principal = parseFloat(document.getElementById('loan-principal').value) || 0;
        const months = parseInt(document.getElementById('loan-tenure').value) || 12;

        const rates = { PERSONAL: 11.5, HOME: 8.5, AUTO: 9.0, EDUCATION: 7.5 };
        const rate = rates[type] || 10.0;

        const r = (rate / 12) / 100;
        const emi = (principal * r * Math.pow(1 + r, months)) / (Math.pow(1 + r, months) - 1);
        const total = emi * months;

        document.getElementById('loan-calc-emi').textContent = formatINR(emi);
        document.getElementById('loan-calc-total').textContent = formatINR(total);
    };

    const onApplyLoan = async (e) => {
        e.preventDefault();
        const acc = document.getElementById('loan-account-select').value;
        const type = document.getElementById('loan-type-select').value;
        const principal = parseFloat(document.getElementById('loan-principal').value);
        const term = parseInt(document.getElementById('loan-tenure').value);

        try {
            const loan = await API.applyLoan(acc, type, principal, term);
            showToast(`Loan approved! ${formatINR(principal)} disbursed into your account.`, 'success');
            loadLoans();
            loadDashboard();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const openRepayLoanModal = (loanId, loanNumber, remainingAmount, suggestedEMI) => {
        document.getElementById('repay-loan-id').value = loanId;
        document.getElementById('repay-loan-number').value = loanNumber;
        document.getElementById('repay-loan-remaining').value = formatINR(remainingAmount);
        document.getElementById('repay-amount').value = Math.min(suggestedEMI, remainingAmount);
        document.getElementById('modal-repay-loan').classList.add('open');
    };

    const onConfirmRepayLoan = async (e) => {
        e.preventDefault();
        const loanId = document.getElementById('repay-loan-id').value;
        const amount = parseFloat(document.getElementById('repay-amount').value);

        try {
            await API.repayLoan(loanId, amount);
            showToast(`Loan repayment of ${formatINR(amount)} successful!`, 'success');
            closeModals();
            loadLoans();
            loadDashboard();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    // -------------------------------------------------------------
    // Wealth & Investments
    // -------------------------------------------------------------
    const loadInvestments = async () => {
        try {
            const [accounts, investments] = await Promise.all([
                API.getAccounts(),
                API.getInvestments()
            ]);
            currentAccounts = accounts;
            currentInvestments = investments;

            const accSelect = document.getElementById('inv-account-select');
            accSelect.innerHTML = accounts.map(a =>
                `<option value="${a.accountNumber}">${a.accountNumber} (${a.accountType}) — Bal: ${formatINR(a.balance)}</option>`
            ).join('');

            recalculateInvReturns();

            const container = document.getElementById('investments-list-container');
            if (!investments || investments.length === 0) {
                container.innerHTML = '<p style="color: var(--text-muted); text-align: center; padding: 2rem;">No investments started yet.</p>';
                return;
            }

            container.innerHTML = investments.map(inv => {
                const isActive = inv.status === 'ACTIVE';
                const statusBadge = isActive
                    ? `<span class="badge badge-success">ACTIVE</span>`
                    : `<span class="badge badge-info">WITHDRAWN</span>`;

                return `
                    <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-glass); border-radius: var(--radius-md); padding: 1.25rem; margin-bottom: 1rem;">
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                            <div>
                                <span style="font-weight: 700; font-size: 1.1rem;">${inv.investmentType.replace('_', ' ')}</span>
                                <div style="font-family: monospace; font-size: 0.8rem; color: #818cf8;">${inv.investmentNumber}</div>
                            </div>
                            ${statusBadge}
                        </div>
                        <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; font-size: 0.85rem; margin-bottom: 1rem;">
                            <div>
                                <div style="color: var(--text-muted);">Principal Invested</div>
                                <div style="font-weight: 700;">${formatINR(inv.investedAmount)}</div>
                            </div>
                            <div>
                                <div style="color: var(--text-muted);">Return Rate</div>
                                <div style="font-weight: 700; color: var(--accent-gold);">${inv.returnRate}% p.a.</div>
                            </div>
                            <div>
                                <div style="color: var(--text-muted);">Maturity Value</div>
                                <div style="font-weight: 700; color: var(--primary);">${formatINR(inv.expectedMaturityAmount)}</div>
                            </div>
                        </div>
                        ${isActive ? `
                            <button class="btn btn-secondary btn-sm" style="width: 100%;" onclick="App.onWithdrawInvestment(${inv.id})">
                                Liquidate & Payout (${formatINR(inv.expectedMaturityAmount)})
                            </button>
                        ` : `
                            <div style="font-size: 0.8rem; color: var(--text-muted); text-align: center;">Liquidated to ${inv.accountNumber}</div>
                        `}
                    </div>
                `;
            }).join('');
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const recalculateInvReturns = () => {
        const type = document.getElementById('inv-type-select').value;
        const amount = parseFloat(document.getElementById('inv-amount').value) || 0;
        const months = parseInt(document.getElementById('inv-months').value) || 12;

        const rates = { FIXED_DEPOSIT: 7.25, MUTUAL_FUND: 12.0, BONDS: 8.1, RECURRING_DEPOSIT: 6.8 };
        const rate = rates[type] || 7.0;

        const returns = (amount * rate * months) / 1200;
        const maturity = amount + returns;

        document.getElementById('inv-calc-rate').textContent = `${rate}% p.a.`;
        document.getElementById('inv-calc-maturity').textContent = formatINR(maturity);
    };

    const onCreateInvestment = async (e) => {
        e.preventDefault();
        const acc = document.getElementById('inv-account-select').value;
        const type = document.getElementById('inv-type-select').value;
        const amount = parseFloat(document.getElementById('inv-amount').value);
        const term = parseInt(document.getElementById('inv-months').value);

        try {
            await API.createInvestment(acc, type, amount, term);
            showToast(`Investment created! ${formatINR(amount)} allocated to ${type}.`, 'success');
            loadInvestments();
            loadDashboard();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const onWithdrawInvestment = async (id) => {
        if (!confirm('Confirm liquidation of this investment? Funds including interest will be credited to your bank account.')) return;
        try {
            await API.withdrawInvestment(id);
            showToast('Investment liquidated successfully! Proceeds credited to account.', 'success');
            loadInvestments();
            loadDashboard();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    // -------------------------------------------------------------
    // Full Transaction Ledger
    // -------------------------------------------------------------
    const loadTransactions = async () => {
        try {
            const txns = await API.getAllTransactions();
            currentTransactions = txns;
            filterTransactions();
        } catch (err) {
            showToast(err.message, 'error');
        }
    };

    const renderFullTransactions = (txns) => {
        const tbody = document.getElementById('full-transactions-table-body');
        if (!tbody) return;

        if (!txns || txns.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2rem;">No transaction records found matching filters.</td></tr>';
            return;
        }

        tbody.innerHTML = txns.map(t => {
            const isCredit = t.transactionType === 'DEPOSIT' || t.description?.includes('Disbursement');
            const amtColor = isCredit ? 'var(--accent-success)' : 'var(--text-primary)';
            const prefix = isCredit ? '+' : '-';

            return `
                <tr>
                    <td><strong style="color: var(--primary); font-family: monospace;">${t.referenceNumber}</strong></td>
                    <td style="font-size: 0.85rem; color: var(--text-secondary);">${formatDate(t.timestamp)}</td>
                    <td><span class="badge badge-info">${t.transactionType}</span></td>
                    <td style="font-family: monospace;">${t.sourceAccountNumber || '-'}</td>
                    <td style="font-family: monospace;">${t.targetAccountNumber || '-'}</td>
                    <td style="font-weight: 700; color: ${amtColor};">${prefix}${formatINR(t.amount)}</td>
                    <td>${t.balanceAfter != null ? formatINR(t.balanceAfter) : '-'}</td>
                    <td><span class="badge badge-success">${t.status}</span></td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="App.viewReceipt('${t.referenceNumber}')">Receipt</button>
                    </td>
                </tr>
            `;
        }).join('');
    };

    const getFilteredTransactions = () => {
        const searchInput = document.getElementById('txn-search-input');
        const q = (searchInput ? searchInput.value : '').toLowerCase().trim();
        const dateFromInput = document.getElementById('txn-date-from');
        const dateToInput = document.getElementById('txn-date-to');
        const fromDate = dateFromInput && dateFromInput.value ? new Date(dateFromInput.value) : null;
        const toDate = dateToInput && dateToInput.value ? new Date(dateToInput.value + 'T23:59:59') : null;

        return (currentTransactions || []).filter(t => {
            const matchesText = !q || (
                (t.referenceNumber && t.referenceNumber.toLowerCase().includes(q)) ||
                (t.sourceAccountNumber && t.sourceAccountNumber.toLowerCase().includes(q)) ||
                (t.targetAccountNumber && t.targetAccountNumber.toLowerCase().includes(q)) ||
                (t.description && t.description.toLowerCase().includes(q)) ||
                (t.transactionType && t.transactionType.toLowerCase().includes(q))
            );

            let matchesDate = true;
            if (t.timestamp) {
                const txnDate = new Date(t.timestamp);
                if (fromDate && txnDate < fromDate) matchesDate = false;
                if (toDate && txnDate > toDate) matchesDate = false;
            }

            return matchesText && matchesDate;
        });
    };

    const filterTransactions = () => {
        const filtered = getFilteredTransactions();
        renderFullTransactions(filtered);
    };

    const resetTxnFilters = () => {
        const searchInput = document.getElementById('txn-search-input');
        const dateFromInput = document.getElementById('txn-date-from');
        const dateToInput = document.getElementById('txn-date-to');
        if (searchInput) searchInput.value = '';
        if (dateFromInput) dateFromInput.value = '';
        if (dateToInput) dateToInput.value = '';
        filterTransactions();
    };

    const exportTransactionsCSV = () => {
        const txnsToExport = getFilteredTransactions();
        if (!txnsToExport || txnsToExport.length === 0) {
            showToast('No transactions to export for current filters.', 'error');
            return;
        }

        const headers = ['Reference Number', 'Timestamp', 'Type', 'Source Account', 'Target Account', 'Amount', 'Currency', 'Balance After', 'Status', 'Description'];
        const csvRows = [headers.join(',')];

        txnsToExport.forEach(t => {
            const row = [
                `"${t.referenceNumber || ''}"`,
                `"${t.timestamp ? formatDate(t.timestamp) : ''}"`,
                `"${t.transactionType || ''}"`,
                `"${t.sourceAccountNumber || ''}"`,
                `"${t.targetAccountNumber || ''}"`,
                `"${t.amount != null ? t.amount : ''}"`,
                `"${t.currency || 'INR'}"`,
                `"${t.balanceAfter != null ? t.balanceAfter : ''}"`,
                `"${t.status || ''}"`,
                `"${(t.description || '').replace(/"/g, '""')}"`
            ];
            csvRows.push(row.join(','));
        });

        const csvContent = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csvRows.join('\n'));
        const link = document.createElement('a');
        link.setAttribute('href', csvContent);
        const dateStr = new Date().toISOString().split('T')[0];
        link.setAttribute('download', `bankease-statement-${dateStr}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        showToast(`Exported ${txnsToExport.length} transactions to CSV!`, 'success');
    };

    // -------------------------------------------------------------
    // API Documentation Renderer
    // -------------------------------------------------------------
    const renderApiDocs = () => {
        const container = document.getElementById('api-docs-container');
        if (!container) return;

        const endpoints = [
            { method: 'POST', path: '/api/auth/register', desc: 'Register a new customer profile with BCrypt password hashing', auth: 'Public' },
            { method: 'POST', path: '/api/auth/login', desc: 'Authenticate credentials and obtain JWT Bearer token', auth: 'Public' },
            { method: 'POST', path: '/api/accounts', desc: 'Open a bank account with optional opening deposit', auth: 'Bearer JWT' },
            { method: 'GET', path: '/api/accounts', desc: 'List all accounts owned by authenticated customer', auth: 'Bearer JWT' },
            { method: 'GET', path: '/api/accounts/{number}/balance', desc: 'Check verified real-time account balance', auth: 'Bearer JWT' },
            { method: 'POST', path: '/api/accounts/{number}/deposit', desc: 'Deposit funds into an account', auth: 'Bearer JWT' },
            { method: 'POST', path: '/api/transfers', desc: 'Execute secure fund transfer with pessimistic locking and idempotency', auth: 'Bearer JWT' },
            { method: 'GET', path: '/api/transactions', desc: 'View global ledger of all user transactions', auth: 'Bearer JWT' },
            { method: 'GET', path: '/api/transactions/receipt/{ref}', desc: 'Retrieve formal transaction confirmation receipt', auth: 'Bearer JWT' },
            { method: 'GET', path: '/api/beneficiaries', desc: 'Retrieve saved payees for quick transfers', auth: 'Bearer JWT' },
            { method: 'POST', path: '/api/beneficiaries', desc: 'Save a new beneficiary account', auth: 'Bearer JWT' },
            { method: 'DELETE', path: '/api/beneficiaries/{id}', desc: 'Remove a beneficiary', auth: 'Bearer JWT' },
            { method: 'GET', path: '/api/bills', desc: 'List pending and paid utility bills', auth: 'Bearer JWT' },
            { method: 'POST', path: '/api/bills', desc: 'Register an upcoming utility or service bill', auth: 'Bearer JWT' },
            { method: 'POST', path: '/api/bills/{id}/pay', desc: 'Pay bill from account with transaction audit', auth: 'Bearer JWT' },
            { method: 'GET', path: '/api/loans', desc: 'List active and repaid loans', auth: 'Bearer JWT' },
            { method: 'POST', path: '/api/loans/apply', desc: 'Apply for loan with instant approval & account disbursement', auth: 'Bearer JWT' },
            { method: 'POST', path: '/api/loans/{id}/repay', desc: 'Pay loan monthly installment or full repayment', auth: 'Bearer JWT' },
            { method: 'GET', path: '/api/investments', desc: 'View investment portfolio valuation', auth: 'Bearer JWT' },
            { method: 'POST', path: '/api/investments', desc: 'Start an investment (FD, Mutual Funds, Bonds)', auth: 'Bearer JWT' },
            { method: 'POST', path: '/api/investments/{id}/withdraw', desc: 'Liquidate investment and credit principal + returns', auth: 'Bearer JWT' }
        ];

        container.innerHTML = `
            <div style="display: flex; flex-direction: column; gap: 1rem;">
                ${endpoints.map(e => `
                    <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-glass); border-radius: var(--radius-md); padding: 1rem 1.25rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
                        <div style="display: flex; align-items: center; gap: 0.75rem;">
                            <span class="badge ${e.method === 'GET' ? 'badge-info' : e.method === 'POST' ? 'badge-success' : 'badge-danger'}" style="min-width: 65px; justify-content: center;">${e.method}</span>
                            <code style="font-size: 1rem; color: var(--text-primary);">${e.path}</code>
                        </div>
                        <div style="font-size: 0.85rem; color: var(--text-secondary);">${e.desc}</div>
                        <div><span class="badge" style="background: rgba(255,255,255,0.08);">${e.auth}</span></div>
                    </div>
                `).join('')}
            </div>
        `;
    };

    const closeModals = () => {
        document.querySelectorAll('.modal-backdrop').forEach(m => m.classList.remove('open'));
    };

    // Close modal on backdrop click
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal-backdrop')) {
            closeModals();
        }
    });

    // Listen for unauthorized events from API
    window.addEventListener('auth:unauthorized', () => {
        showToast('Your session has expired. Please sign in again.', 'info');
        updateAuthUI();
    });

    // Boot
    document.addEventListener('DOMContentLoaded', () => {
        updateAuthUI();
    });

    return {
        navigate,
        handleAuthButton,
        switchAuthTab,
        togglePasswordVisibility,
        openProfileModal,
        onUpdateProfile,
        onLogin,
        onRegister,
        onLogout,
        fillDemoCredentials,
        openCreateAccountModal,
        onCreateAccount,
        openDepositModal,
        onDeposit,
        loadTransfersView,
        onTransferSourceChange,
        onBeneficiarySelect,
        setTransferAmount,
        onExecuteTransfer,
        viewReceipt,
        copyReceiptRef,
        onAddBeneficiary,
        onDeleteBeneficiary,
        transferToBeneficiary,
        loadBills,
        filterBills,
        onCreateBill,
        openPayBillModal,
        onConfirmPayBill,
        recalculateLoanEMI,
        onApplyLoan,
        openRepayLoanModal,
        onConfirmRepayLoan,
        recalculateInvReturns,
        onCreateInvestment,
        onWithdrawInvestment,
        filterTransactions,
        resetTxnFilters,
        exportTransactionsCSV,
        closeModals
    };
})();
