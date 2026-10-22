// ==========================================================================
// NEXUS CORE - Customer Banking Dashboard Operations Controller
// ==========================================================================

let currentAccount = null;
let allTransactions = [];
let activeFilter = 'ALL';

const token = sessionStorage.getItem('bank_token');
const userStr = sessionStorage.getItem('bank_user');

if (!token || !userStr) {
    window.location.href = '/index.html';
}

const user = JSON.parse(userStr);

document.addEventListener('DOMContentLoaded', () => {
    const fullName = user.fullName || user.username;
    document.getElementById('navUserName').innerText = fullName;
    document.getElementById('navUserRole').innerText = user.role;
    document.getElementById('navAvatar').innerText = fullName.charAt(0).toUpperCase();
    document.getElementById('displayHolderName').innerText = fullName.toUpperCase();

    loadAccountData();
});

// Toast System
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let iconSvg = '';
    if (type === 'success') {
        iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>`;
    } else if (type === 'error') {
        iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f43f5e" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`;
    } else {
        iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#0ea5e9" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
    }

    toast.innerHTML = `
        <div style="flex-shrink: 0; margin-top: 1px;">${iconSvg}</div>
        <div style="flex: 1; line-height: 1.4;">${message}</div>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.25s ease';
        setTimeout(() => toast.remove(), 250);
    }, 4500);
}

function showAlert(message, type = 'error') {
    const box = document.getElementById('alertBox');
    if (!box) return;
    box.className = `alert alert-${type} show`;
    box.innerHTML = `
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0;">
            ${type === 'success' ? '<polyline points="20 6 9 17 4 12"></polyline>' : '<circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line>'}
        </svg>
        <span>${message}</span>
    `;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function clearAlert() {
    const box = document.getElementById('alertBox');
    if (!box) return;
    box.className = 'alert';
    box.innerHTML = '';
}

function logout() {
    fetch('/api/auth/logout', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    }).finally(() => {
        sessionStorage.clear();
        window.location.href = '/index.html';
    });
}

function formatCurrency(amount) {
    if (amount === undefined || amount === null) return '₹ 0.00';
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    }).format(amount);
}

function copyAccountNumber() {
    if (currentAccount && currentAccount.accountNumber) {
        navigator.clipboard.writeText(currentAccount.accountNumber).then(() => {
            showToast(`Account number ${currentAccount.accountNumber} copied to clipboard!`, 'success');
        });
    }
}

function fillDeposit(amt) {
    const input = document.getElementById('depositAmount');
    if (input) {
        input.value = amt;
        input.focus();
    }
    const box = document.getElementById('depositFeedbackBox');
    if (box) box.style.display = 'none';
}

function fillWithdraw(amt) {
    const input = document.getElementById('withdrawAmount');
    if (input) {
        input.value = amt;
        input.focus();
    }
    const box = document.getElementById('withdrawFeedbackBox');
    if (box) box.style.display = 'none';
}

function showDepositFeedback(html, type = 'error') {
    const box = document.getElementById('depositFeedbackBox');
    if (!box) return;
    box.className = `transfer-feedback-panel ${type}`;
    box.innerHTML = html;
    box.style.display = 'block';
}

function showWithdrawFeedback(html, type = 'error') {
    const box = document.getElementById('withdrawFeedbackBox');
    if (!box) return;
    box.className = `transfer-feedback-panel ${type}`;
    box.innerHTML = html;
    box.style.display = 'block';
}

function fillTarget(acc, defaultAmt = 1000) {
    const targetInput = document.getElementById('targetAccount');
    if (targetInput) targetInput.value = acc;
    const amtInput = document.getElementById('transferAmount');
    if (amtInput && (!amtInput.value || parseFloat(amtInput.value) <= 0)) {
        amtInput.value = defaultAmt;
    }
    evaluateTransferRoute();
    const feedbackBox = document.getElementById('transferFeedbackBox');
    if (feedbackBox) feedbackBox.style.display = 'none';
}

function fillTransferAmount(amt) {
    const amtInput = document.getElementById('transferAmount');
    if (amtInput) {
        amtInput.value = amt;
        amtInput.focus();
    }
    const feedbackBox = document.getElementById('transferFeedbackBox');
    if (feedbackBox) feedbackBox.style.display = 'none';
}

function showTransferFeedback(html, type = 'error') {
    const box = document.getElementById('transferFeedbackBox');
    if (!box) return;
    box.className = `transfer-feedback-panel ${type}`;
    box.innerHTML = html;
    box.style.display = 'block';
}

function evaluateTransferRoute() {
    const targetInput = document.getElementById('targetAccount');
    const box = document.getElementById('routeIndicatorBox');
    const text = document.getElementById('routeIndicatorText');

    if (!targetInput || !box || !text) return;

    const target = targetInput.value.trim().toUpperCase();
    if (!target || !currentAccount) {
        box.style.display = 'none';
        return;
    }

    const currentBranchCode = (currentAccount.branch.branchCode || '').toUpperCase();
    const currentPrefix = currentBranchCode.substring(0, 3);
    const targetPrefix = target.substring(0, 3);

    box.style.display = 'flex';

    if (target.toUpperCase() === currentAccount.accountNumber.toUpperCase()) {
        box.className = 'route-detector-box';
        box.style.background = 'rgba(244, 63, 94, 0.12)';
        box.style.borderColor = 'rgba(244, 63, 94, 0.35)';
        box.style.color = '#fb7185';
        text.innerHTML = '⚠️ Source and destination accounts cannot be identical.';
    } else if (targetPrefix === currentPrefix) {
        box.className = 'route-detector-box local';
        text.innerHTML = `🔒 <strong>Intra-Branch Route</strong> (${currentAccount.branch.branchName}). Atomic commit with deterministic locking.`;
    } else {
        let destBranchName = 'Remote Branch';
        if (targetPrefix === 'MUM') destBranchName = 'Mumbai Main (Node 2)';
        else if (targetPrefix === 'DEL') destBranchName = 'Delhi Regional (Node 3)';
        else if (targetPrefix === 'PUN') destBranchName = 'Pune Central (Node 1)';

        box.className = 'route-detector-box twopc';
        text.innerHTML = `⚡ <strong>Cross-Branch 2PC Route</strong>: ${currentAccount.branch.branchName} ➔ ${destBranchName}. Coordinated via Two-Phase Commit over RMI.`;
    }
}

function setBtnLoading(btn, isLoading, originalHtml) {
    if (!btn) return;
    if (isLoading) {
        btn.disabled = true;
        btn.innerHTML = `
            <svg class="spinner" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="2" x2="12" y2="6"></line><line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line><line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
            </svg>
            <span>Processing Consensus...</span>
        `;
    } else {
        btn.disabled = false;
        btn.innerHTML = originalHtml;
    }
}

// Master Account Loader
async function loadAccountData() {
    clearAlert();
    try {
        const response = await fetch('/api/customer/account', {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.status === 401) {
            logout();
            return;
        }

        const accounts = await response.json();
        if (accounts && accounts.length > 0) {
            currentAccount = accounts[0];

            document.getElementById('displayAccountNumber').innerText = currentAccount.accountNumber;
            document.getElementById('displayBalance').innerText = formatCurrency(currentAccount.balance);
            document.getElementById('displayBranchCode').innerText = currentAccount.branch.branchCode;
            document.getElementById('displayBranchBadge').innerText = currentAccount.branch.branchCode;
            document.getElementById('displayBranchLocation').innerText = `${currentAccount.branch.branchName} • Node ${currentAccount.branch.id}`;
            document.getElementById('navBranchPill').innerText = currentAccount.branch.branchName.toUpperCase();

            loadTransactions();
        } else {
            showAlert('No active bank account currently linked to this customer profile.', 'error');
        }
    } catch (err) {
        showAlert('Cluster connection error: ' + err.message, 'error');
    }
}

// Deposit
async function handleDeposit(event) {
    event.preventDefault();
    clearAlert();

    const input = document.getElementById('depositAmount');
    const amount = input ? parseFloat(input.value) : 0;
    if (!amount || amount <= 0) {
        showDepositFeedback('Please enter a valid deposit amount greater than ₹0.', 'error');
        if (input) input.focus();
        return;
    }

    const btn = document.getElementById('btnDepositSubmit');
    const originalText = btn.innerHTML;
    setBtnLoading(btn, true, originalText);

    showDepositFeedback(`
        <div style="display: flex; align-items: center; gap: 0.5rem; font-weight: 600;">
            <svg class="spinner" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="2" x2="12" y2="6"></line><line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line><line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
            </svg>
            <span>Processing Deposit with Row-Level Lock...</span>
        </div>
    `, 'loading');

    const idempotencyKey = 'DEP-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7);

    try {
        const response = await fetch('/api/customer/deposit', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                accountNumber: currentAccount.accountNumber,
                amount: amount,
                idempotencyKey: idempotencyKey
            })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showDepositFeedback(`
                <div class="feedback-success-header">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#34d399" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                    <span>Deposit Credited Successfully!</span>
                </div>
                <div class="feedback-details-grid">
                    <div><strong>Credited:</strong> <span style="color: #34d399; font-weight: 700;">+${formatCurrency(amount)}</span></div>
                    <div><strong>Txn Reference:</strong> <span class="mono" style="color: #38bdf8; font-weight: 700;">${data.transactionRef}</span></div>
                    <div><strong>Logical Clock:</strong> Lamport L = ${data.lamportTimestamp}</div>
                    <div><strong>New Balance:</strong> <span style="color: #34d399; font-weight: 700;">${formatCurrency(data.newBalance)}</span></div>
                </div>
            `, 'success');

            showToast(`Deposit of ${formatCurrency(amount)} completed! Ref: ${data.transactionRef}`, 'success');
            if (input) input.value = '';

            const balEl = document.getElementById('displayBalance');
            if (balEl) {
                balEl.innerText = formatCurrency(data.newBalance);
                balEl.classList.remove('balance-highlight');
                void balEl.offsetWidth;
                balEl.classList.add('balance-highlight');
            }

            updateLamportTick(data.lamportTimestamp);
            loadTransactions(data.transactionRef);
        } else {
            const errorMsg = data.error || 'Deposit failed.';
            showDepositFeedback(`
                <div class="feedback-error-header">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f43f5e" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    <span>Deposit Failed</span>
                </div>
                <div style="font-size: 0.775rem; margin-top: 0.25rem; color: #ffe4e6;">${errorMsg}</div>
            `, 'error');
            showToast(errorMsg, 'error');
        }
    } catch (err) {
        showDepositFeedback('Deposit communication error: ' + err.message, 'error');
        showToast('Deposit communication error: ' + err.message, 'error');
    } finally {
        setBtnLoading(btn, false, originalText);
    }
}

// Withdrawal
async function handleWithdraw(event) {
    event.preventDefault();
    clearAlert();

    const input = document.getElementById('withdrawAmount');
    const amount = input ? parseFloat(input.value) : 0;
    if (!amount || amount <= 0) {
        showWithdrawFeedback('Please enter a valid withdrawal amount greater than ₹0.', 'error');
        if (input) input.focus();
        return;
    }

    const btn = document.getElementById('btnWithdrawSubmit');
    const originalText = btn.innerHTML;
    setBtnLoading(btn, true, originalText);

    showWithdrawFeedback(`
        <div style="display: flex; align-items: center; gap: 0.5rem; font-weight: 600;">
            <svg class="spinner" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="2" x2="12" y2="6"></line><line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line><line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
            </svg>
            <span>Verifying Concurrency Lock & Balance...</span>
        </div>
    `, 'loading');

    const idempotencyKey = 'WTH-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7);

    try {
        const response = await fetch('/api/customer/withdraw', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                accountNumber: currentAccount.accountNumber,
                amount: amount,
                idempotencyKey: idempotencyKey
            })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showWithdrawFeedback(`
                <div class="feedback-success-header">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#34d399" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                    <span>Withdrawal Debited Successfully!</span>
                </div>
                <div class="feedback-details-grid">
                    <div><strong>Debited:</strong> <span style="color: #fb7185; font-weight: 700;">-${formatCurrency(amount)}</span></div>
                    <div><strong>Txn Reference:</strong> <span class="mono" style="color: #38bdf8; font-weight: 700;">${data.transactionRef}</span></div>
                    <div><strong>Logical Clock:</strong> Lamport L = ${data.lamportTimestamp}</div>
                    <div><strong>Remaining Balance:</strong> <span style="color: #38bdf8; font-weight: 700;">${formatCurrency(data.newBalance)}</span></div>
                </div>
            `, 'success');

            showToast(`Withdrawal of ${formatCurrency(amount)} confirmed! Ref: ${data.transactionRef}`, 'success');
            if (input) input.value = '';

            const balEl = document.getElementById('displayBalance');
            if (balEl) {
                balEl.innerText = formatCurrency(data.newBalance);
                balEl.classList.remove('balance-highlight');
                void balEl.offsetWidth;
                balEl.classList.add('balance-highlight');
            }

            updateLamportTick(data.lamportTimestamp);
            loadTransactions(data.transactionRef);
        } else {
            const errorMsg = data.error || 'Withdrawal failed.';
            showWithdrawFeedback(`
                <div class="feedback-error-header">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f43f5e" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    <span>Withdrawal Rejected</span>
                </div>
                <div style="font-size: 0.775rem; margin-top: 0.25rem; color: #ffe4e6;">${errorMsg}</div>
            `, 'error');
            showToast(errorMsg, 'error');
        }
    } catch (err) {
        showWithdrawFeedback('Withdrawal communication error: ' + err.message, 'error');
        showToast('Withdrawal communication error: ' + err.message, 'error');
    } finally {
        setBtnLoading(btn, false, originalText);
    }
}

// Transfer
async function handleTransfer(event) {
    event.preventDefault();
    clearAlert();

    const targetInput = document.getElementById('targetAccount');
    const amountInput = document.getElementById('transferAmount');
    const targetAccount = targetInput ? targetInput.value.trim().toUpperCase() : '';
    const amount = amountInput ? parseFloat(amountInput.value) : 0;

    if (!targetAccount) {
        showTransferFeedback('Please enter or select a destination account number.', 'error');
        if (targetInput) targetInput.focus();
        return;
    }
    if (targetAccount === currentAccount.accountNumber.toUpperCase()) {
        showTransferFeedback('Source and destination accounts cannot be identical.', 'error');
        if (targetInput) targetInput.focus();
        return;
    }
    if (!amount || amount <= 0) {
        showTransferFeedback('Please enter a valid transfer amount greater than ₹0.', 'error');
        if (amountInput) amountInput.focus();
        return;
    }

    const btn = document.getElementById('btnTransferSubmit');
    const originalText = btn.innerHTML;
    setBtnLoading(btn, true, originalText);

    // Show dynamic in-card 2PC progress indicator
    showTransferFeedback(`
        <div style="display: flex; align-items: center; gap: 0.5rem; font-weight: 600;">
            <svg class="spinner" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="2" x2="12" y2="6"></line><line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line><line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
            </svg>
            <span>Executing Distributed Transfer...</span>
        </div>
        <div style="font-size: 0.725rem; color: #7dd3fc; margin-top: 0.25rem;">
            Phase 1 (Prepare): Requesting votes & locking ${currentAccount.accountNumber} & ${targetAccount} via RMI stubs
        </div>
    `, 'loading');

    const idempotencyKey = 'XFER-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7);

    try {
        const response = await fetch('/api/customer/transfer', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                fromAccount: currentAccount.accountNumber,
                toAccount: targetAccount,
                amount: amount,
                idempotencyKey: idempotencyKey
            })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            const is2pc = data.transactionType === 'TRANSFER_DISTRIBUTED';
            const protocolText = is2pc ? 'Two-Phase Commit (2PC)' : 'Local Atomic Transfer';

            showTransferFeedback(`
                <div class="feedback-success-header">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#34d399" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                    <span>Transfer Executed Successfully!</span>
                </div>
                <div class="feedback-details-grid">
                    <div><strong>Protocol:</strong> <span style="color: #38bdf8;">${protocolText}</span></div>
                    <div><strong>Transferred:</strong> ${formatCurrency(amount)} ➔ <span class="mono">${targetAccount}</span></div>
                    <div><strong>Txn Reference:</strong> <span class="mono" style="color: #38bdf8; font-weight: 700;">${data.transactionRef}</span></div>
                    <div><strong>Consensus Clock:</strong> Lamport L = ${data.lamportTimestamp}</div>
                    <div><strong>Updated Balance:</strong> <span style="color: #34d399; font-weight: 700;">${formatCurrency(data.newBalance)}</span></div>
                </div>
            `, 'success');

            showToast(`Transferred ${formatCurrency(amount)} to ${targetAccount} via ${protocolText}!`, 'success');

            // Animate card balance
            const balEl = document.getElementById('displayBalance');
            if (balEl) {
                balEl.innerText = formatCurrency(data.newBalance);
                balEl.classList.remove('balance-highlight');
                void balEl.offsetWidth;
                balEl.classList.add('balance-highlight');
            }

            document.getElementById('targetAccount').value = '';
            document.getElementById('transferAmount').value = '';
            document.getElementById('routeIndicatorBox').style.display = 'none';

            updateLamportTick(data.lamportTimestamp);
            loadTransactions(data.transactionRef);
        } else {
            const errorMsg = data.error || data.failureReason || 'Transfer was rejected by the banking cluster.';
            showTransferFeedback(`
                <div class="feedback-error-header">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f43f5e" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    <span>Transfer Rejected</span>
                </div>
                <div style="font-size: 0.775rem; margin-top: 0.25rem; color: #ffe4e6;">
                    ${errorMsg}
                </div>
            `, 'error');
            showToast(errorMsg, 'error');
            loadAccountData();
        }
    } catch (err) {
        showTransferFeedback(`
            <div class="feedback-error-header">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f43f5e" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <span>Communication Failure</span>
            </div>
            <div style="font-size: 0.775rem; margin-top: 0.25rem;">
                Cluster communication error: ${err.message}
            </div>
        `, 'error');
        showToast('Transfer communication error: ' + err.message, 'error');
    } finally {
        setBtnLoading(btn, false, originalText);
    }
}

function updateLamportTick(ts) {
    if (ts !== undefined && ts !== null) {
        document.getElementById('displayLamportTick').innerText = `L = ${ts}`;
        document.getElementById('navLamportTick').innerText = `L = ${ts}`;
    }
}

let latestHighlightRef = null;

// Load Transactions
async function loadTransactions(highlightRef = null) {
    if (!currentAccount) return;
    if (highlightRef) latestHighlightRef = highlightRef;

    try {
        const response = await fetch(`/api/customer/transactions?accountNumber=${encodeURIComponent(currentAccount.accountNumber)}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        allTransactions = await response.json() || [];
        renderLedgerTable();
    } catch (err) {
        console.error('Failed to load transaction ledger:', err);
    }
}

function setLedgerFilter(filter, btn) {
    activeFilter = filter;
    document.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
    if (btn) btn.classList.add('active');
    renderLedgerTable();
}

function filterLedger() {
    renderLedgerTable();
}

function renderLedgerTable() {
    const tbody = document.getElementById('transactionsTableBody');
    if (!tbody) return;

    const query = (document.getElementById('ledgerSearchInput')?.value || '').toLowerCase().trim();

    let filtered = allTransactions.filter(t => {
        if (activeFilter === 'ALL') return true;
        return t.transactionType === activeFilter;
    });

    if (query) {
        filtered = filtered.filter(t => {
            const ref = (t.transactionRef || '').toLowerCase();
            const src = (t.sourceAccountNumber || '').toLowerCase();
            const dst = (t.destinationAccountNumber || '').toLowerCase();
            const type = (t.transactionType || '').toLowerCase();
            const amt = String(t.amount || '');
            return ref.includes(query) || src.includes(query) || dst.includes(query) || type.includes(query) || amt.includes(query);
        });
    }

    if (!filtered || filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; color: var(--text-muted); padding: 2.5rem;">
                    No ledger records match the selected filter criteria.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filtered.map(t => {
        let typeBadgeClass = 'badge-protocol';
        let typeName = t.transactionType;

        if (t.transactionType === 'DEPOSIT') {
            typeBadgeClass = 'badge-running';
            typeName = 'Deposit';
        } else if (t.transactionType === 'WITHDRAWAL') {
            typeBadgeClass = 'badge-crashed';
            typeName = 'Withdrawal';
        } else if (t.transactionType === 'TRANSFER_DISTRIBUTED') {
            typeBadgeClass = 'badge-coordinator';
            typeName = '2PC Cross-Branch';
        } else if (t.transactionType === 'TRANSFER_LOCAL') {
            typeBadgeClass = 'badge-protocol';
            typeName = 'Local Transfer';
        }

        const isSuccess = t.status === 'SUCCESS';
        const dateStr = t.createdAt ? t.createdAt.replace('T', ' ').substring(0, 19) : 'Just now';

        let counterparty = '-';
        if (t.transactionType === 'TRANSFER_LOCAL' || t.transactionType === 'TRANSFER_DISTRIBUTED') {
            counterparty = t.sourceAccountNumber === currentAccount.accountNumber
                ? `<span style="color: #fb7185;">To:</span> ${t.destinationAccountNumber}`
                : `<span style="color: #34d399;">From:</span> ${t.sourceAccountNumber}`;
        }

        const isDebit = (t.transactionType === 'WITHDRAWAL') ||
            (t.transactionType.startsWith('TRANSFER') && t.sourceAccountNumber === currentAccount.accountNumber);

        const amountColor = isDebit ? '#fb7185' : '#34d399';
        const amountPrefix = isDebit ? '-' : '+';
        const isHighlighted = (latestHighlightRef && t.transactionRef === latestHighlightRef);

        return `
            <tr class="${isHighlighted ? 'row-highlight' : ''}">
                <td class="mono" style="font-weight: 700; color: #ffffff;">${t.transactionRef}</td>
                <td class="mono" style="color: var(--text-muted); font-size: 0.775rem;">${dateStr}</td>
                <td><span class="status-badge ${typeBadgeClass}" style="font-size: 0.68rem;">${typeName}</span></td>
                <td class="mono" style="font-size: 0.825rem;">${counterparty}</td>
                <td class="align-right mono" style="font-weight: 800; font-size: 0.95rem; color: ${amountColor};">
                    ${amountPrefix} ${formatCurrency(t.amount)}
                </td>
                <td class="mono" style="color: #c084fc; font-weight: 600;">L = ${t.lamportTimestamp}</td>
                <td>
                    <span class="status-badge ${isSuccess ? 'badge-running' : 'badge-crashed'}" style="font-size: 0.68rem;">
                        ${t.status}
                    </span>
                </td>
            </tr>
        `;
    }).join('');
}

function exportTransactionsCSV() {
    if (!allTransactions || allTransactions.length === 0) {
        showToast('No transactions available to export.', 'warning');
        return;
    }

    const headers = ['Reference ID', 'Timestamp', 'Type', 'Source Account', 'Dest Account', 'Amount', 'Status', 'Lamport Clock'];
    const rows = allTransactions.map(t => [
        t.transactionRef,
        t.createdAt ? t.createdAt.replace('T', ' ').substring(0, 19) : '',
        t.transactionType,
        t.sourceAccountNumber || '',
        t.destinationAccountNumber || '',
        t.amount,
        t.status,
        t.lamportTimestamp
    ]);

    let csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `Nexus_Statement_${currentAccount.accountNumber}_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    showToast('Passbook statement exported to CSV successfully.', 'success');
}
