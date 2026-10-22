// ==========================================================================
// NEXUS CORE - Authentication & Portal Navigation Logic
// ==========================================================================

function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    if (tab === 'customer') {
        document.getElementById('tabCustomerBtn').classList.add('active');
        document.getElementById('customerTab').classList.add('active');
    } else {
        document.getElementById('tabAdminBtn').classList.add('active');
        document.getElementById('adminTab').classList.add('active');
    }
    clearAlert();
}

function toggleCustomerRegister(showRegister) {
    const loginForm = document.getElementById('customerLoginForm');
    const registerForm = document.getElementById('customerRegisterForm');
    const title = document.getElementById('customerFormTitle');

    if (showRegister) {
        loginForm.style.display = 'none';
        registerForm.style.display = 'block';
        title.innerText = 'Open Verified Account';
    } else {
        loginForm.style.display = 'block';
        registerForm.style.display = 'none';
        title.innerText = 'Customer Sign In';
    }
    clearAlert();
}

function fillCustomer(username, password) {
    document.getElementById('custUsername').value = username;
    document.getElementById('custPassword').value = password;
    clearAlert();
}

function fillAdmin(username, password) {
    document.getElementById('adminUsername').value = username;
    document.getElementById('adminPassword').value = password;
    clearAlert();
}

function showAlert(message, type = 'error') {
    const box = document.getElementById('alertBox');
    if (!box) return;
    box.className = `alert alert-${type} show`;
    box.innerHTML = `
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0;">
            ${type === 'success' 
                ? '<polyline points="20 6 9 17 4 12"></polyline>' 
                : '<circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line>'}
        </svg>
        <span>${message}</span>
    `;
}

function clearAlert() {
    const box = document.getElementById('alertBox');
    if (!box) return;
    box.className = 'alert';
    box.innerHTML = '';
}

function setBtnLoading(btn, isLoading, originalHtml) {
    if (!btn) return;
    if (isLoading) {
        btn.disabled = true;
        btn.innerHTML = `
            <svg class="spinner" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="2" x2="12" y2="6"></line><line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line><line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
            </svg>
            <span>Verifying Credentials...</span>
        `;
    } else {
        btn.disabled = false;
        btn.innerHTML = originalHtml;
    }
}

async function handleCustomerLogin(event) {
    event.preventDefault();
    clearAlert();

    const username = document.getElementById('custUsername').value.trim();
    const password = document.getElementById('custPassword').value;
    const btn = document.getElementById('btnCustLogin');
    const originalText = btn.innerHTML;

    setBtnLoading(btn, true, originalText);

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (response.ok && data.token) {
            sessionStorage.setItem('bank_token', data.token);
            sessionStorage.setItem('bank_user', JSON.stringify(data));
            window.location.href = '/customer.html';
        } else {
            showAlert(data.error || 'Authentication rejected. Please check username and password.', 'error');
        }
    } catch (err) {
        showAlert('Cluster connection error: ' + err.message, 'error');
    } finally {
        setBtnLoading(btn, false, originalText);
    }
}

async function handleCustomerRegister(event) {
    event.preventDefault();
    clearAlert();

    const username = document.getElementById('regUsername').value.trim();
    const fullName = document.getElementById('regFullName').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const branchId = document.getElementById('regBranch').value;
    const password = document.getElementById('regPassword').value;
    const btn = document.getElementById('btnCustRegister');
    const originalText = btn.innerHTML;

    setBtnLoading(btn, true, originalText);

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, fullName, email, branchId, password })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showAlert('Registration verified and savings account provisioned! Please sign in.', 'success');
            toggleCustomerRegister(false);
            document.getElementById('custUsername').value = username;
            document.getElementById('custPassword').value = password;
        } else {
            showAlert(data.error || 'Account creation rejected by validation rules.', 'error');
        }
    } catch (err) {
        showAlert('Registration transport failure: ' + err.message, 'error');
    } finally {
        setBtnLoading(btn, false, originalText);
    }
}

async function handleAdminLogin(event) {
    event.preventDefault();
    clearAlert();

    const username = document.getElementById('adminUsername').value.trim();
    const password = document.getElementById('adminPassword').value;
    const btn = document.getElementById('btnAdminLogin');
    const originalText = btn.innerHTML;

    setBtnLoading(btn, true, originalText);

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (response.ok && data.token) {
            if (data.role !== 'ROLE_ADMIN') {
                showAlert('Access Forbidden: Administrator privileges required for this console.', 'error');
                return;
            }
            sessionStorage.setItem('bank_token', data.token);
            sessionStorage.setItem('bank_user', JSON.stringify(data));
            window.location.href = '/admin.html';
        } else {
            showAlert(data.error || 'Supervisor credentials rejected.', 'error');
        }
    } catch (err) {
        showAlert('Network or cluster connection error: ' + err.message, 'error');
    } finally {
        setBtnLoading(btn, false, originalText);
    }
}
