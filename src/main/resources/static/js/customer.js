// Customer Dashboard Controller

let currentAccount = null;
const token = sessionStorage.getItem('bank_token');
const userStr = sessionStorage.getItem('bank_user');

if (!token || !userStr) {
    window.location.href = '/index.html';
}

const user = JSON.parse(userStr);

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('navUserName').innerText = user.fullName || user.username;
    document.getElementById('navUserRole').innerText = user.role;
    loadAccountData();
});

function showAlert(message, type = 'error') {
    const box = document.getElementById('alertBox');
    box.className = `alert alert-${type} show`;
    box.innerText = message;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function clearAlert() {
    const box = document.getElementById('alertBox');
    box.className = 'alert';
    box.innerText = '';
}

function fillTarget(acc) {
    document.getElementById('targetAccount').value = acc;
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
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    }).format(amount);
}

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
            document.getElementById('displayBranchName').innerText = currentAccount.branch.branchName;
            document.getElementById('displayBranchCode').innerText = currentAccount.branch.branchCode;
            document.getElementById('displayAccountStatus').innerText = currentAccount.status;
            document.getElementById('displayBalance').innerText = formatCurrency(currentAccount.balance);

            loadTransactions();
        } else {
            showAlert('No active bank account linked to this customer profile.', 'error');
        }
    } catch (err) {
        showAlert('Failed to connect to banking service: ' + err.message, 'error');
    }
}

async function handleDeposit(event) {
    event.preventDefault();
    clearAlert();

    const amount = parseFloat(document.getElementById('depositAmount').value);
    if (!amount || amount <= 0) {
        showAlert('Please enter a valid deposit amount.', 'error');
        return;
    }

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
            showAlert(`Deposit of ${formatCurrency(amount)} successful! Ref: ${data.transactionRef} (L-Clock: ${data.lamportTimestamp})`, 'success');
            document.getElementById('depositAmount').value = '';
            document.getElementById('displayBalance').innerText = formatCurrency(data.newBalance);
            loadTransactions();
        } else {
            showAlert(data.error || 'Deposit failed.', 'error');
        }
    } catch (err) {
        showAlert('Deposit communication error: ' + err.message, 'error');
    }
}

async function handleWithdraw(event) {
    event.preventDefault();
    clearAlert();

    const amount = parseFloat(document.getElementById('withdrawAmount').value);
    if (!amount || amount <= 0) {
        showAlert('Please enter a valid withdrawal amount.', 'error');
        return;
    }

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
            showAlert(`Withdrawal of ${formatCurrency(amount)} successful! Ref: ${data.transactionRef} (L-Clock: ${data.lamportTimestamp})`, 'success');
            document.getElementById('withdrawAmount').value = '';
            document.getElementById('displayBalance').innerText = formatCurrency(data.newBalance);
            loadTransactions();
        } else {
            showAlert(data.error || 'Withdrawal failed.', 'error');
        }
    } catch (err) {
        showAlert('Withdrawal communication error: ' + err.message, 'error');
    }
}

async function handleTransfer(event) {
    event.preventDefault();
    clearAlert();

    const targetAccount = document.getElementById('targetAccount').value.trim();
    const amount = parseFloat(document.getElementById('transferAmount').value);

    if (!targetAccount) {
        showAlert('Please enter a destination account number.', 'error');
        return;
    }
    if (!amount || amount <= 0) {
        showAlert('Please enter a valid transfer amount.', 'error');
        return;
    }

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
            showAlert(`Transfer of ${formatCurrency(amount)} to ${targetAccount} completed via ${protocolText}! Ref: ${data.transactionRef} (L-Clock: ${data.lamportTimestamp})`, 'success');

            document.getElementById('targetAccount').value = '';
            document.getElementById('transferAmount').value = '';
            document.getElementById('displayBalance').innerText = formatCurrency(data.newBalance);
            loadTransactions();
        } else {
            showAlert(data.error || data.failureReason || 'Transfer was rejected by the banking cluster.', 'error');
            loadAccountData();
        }
    } catch (err) {
        showAlert('Transfer communication failure: ' + err.message, 'error');
    }
}

async function loadTransactions() {
    if (!currentAccount) return;

    try {
        const response = await fetch(`/api/customer/transactions?accountNumber=${encodeURIComponent(currentAccount.accountNumber)}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const txns = await response.json();
        const tbody = document.getElementById('transactionsTableBody');

        if (!txns || txns.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No transaction records found.</td></tr>';
            return;
        }

        tbody.innerHTML = txns.map(t => {
            let typeBadgeClass = 'badge-primary';
            if (t.transactionType === 'DEPOSIT') typeBadgeClass = 'badge-deposit';
            else if (t.transactionType === 'WITHDRAWAL') typeBadgeClass = 'badge-withdrawal';
            else if (t.transactionType === 'TRANSFER_LOCAL') typeBadgeClass = 'badge-transfer-local';
            else if (t.transactionType === 'TRANSFER_DISTRIBUTED') typeBadgeClass = 'badge-transfer-dist';

            const statusClass = t.status === 'SUCCESS' ? 'badge-success' : 'badge-danger';
            const dateStr = t.createdAt ? t.createdAt.replace('T', ' ').substring(0, 19) : 'Just now';

            let counterparty = '-';
            if (t.transactionType === 'TRANSFER_LOCAL' || t.transactionType === 'TRANSFER_DISTRIBUTED') {
                counterparty = t.sourceAccountNumber === currentAccount.accountNumber
                    ? `To: ${t.destinationAccountNumber}`
                    : `From: ${t.sourceAccountNumber}`;
            }

            const isDebit = (t.transactionType === 'WITHDRAWAL') ||
                ((t.transactionType.startsWith('TRANSFER')) && t.sourceAccountNumber === currentAccount.accountNumber);
            const amountColor = isDebit ? '#dc2626' : '#16a34a';
            const amountPrefix = isDebit ? '-' : '+';

            return `
                <tr>
                    <td class="mono" style="font-size: 0.78rem; font-weight: 600;">${t.transactionRef}</td>
                    <td style="color: var(--text-muted); font-size: 0.8rem;">${dateStr}</td>
                    <td><span class="badge ${typeBadgeClass}">${t.transactionType}</span></td>
                    <td class="mono" style="font-size: 0.8rem;">${counterparty}</td>
                    <td class="mono" style="font-weight: 700; color: ${amountColor};">${amountPrefix} ${formatCurrency(t.amount)}</td>
                    <td class="mono" style="color: var(--text-muted);">${t.lamportTimestamp}</td>
                    <td><span class="badge ${statusClass}">${t.status}</span></td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Failed to load transactions:', err);
    }
}
