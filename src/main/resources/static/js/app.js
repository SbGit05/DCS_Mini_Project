// Authentication & Portal Navigation Logic

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
        title.innerText = 'New Customer Registration';
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
}

function fillAdmin(username, password) {
    document.getElementById('adminUsername').value = username;
    document.getElementById('adminPassword').value = password;
}

function showAlert(message, type = 'error') {
    const box = document.getElementById('alertBox');
    box.className = `alert alert-${type} show`;
    box.innerText = message;
}

function clearAlert() {
    const box = document.getElementById('alertBox');
    box.className = 'alert';
    box.innerText = '';
}

async function handleCustomerLogin(event) {
    event.preventDefault();
    clearAlert();

    const username = document.getElementById('custUsername').value.trim();
    const password = document.getElementById('custPassword').value;

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
            showAlert(data.error || 'Login failed. Please check your credentials.', 'error');
        }
    } catch (err) {
        showAlert('Network or server connection error: ' + err.message, 'error');
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

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, fullName, email, branchId, password })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showAlert('Registration successful! Please sign in with your new credentials.', 'success');
            toggleCustomerRegister(false);
            document.getElementById('custUsername').value = username;
        } else {
            showAlert(data.error || 'Registration failed.', 'error');
        }
    } catch (err) {
        showAlert('Registration connection error: ' + err.message, 'error');
    }
}

async function handleAdminLogin(event) {
    event.preventDefault();
    clearAlert();

    const username = document.getElementById('adminUsername').value.trim();
    const password = document.getElementById('adminPassword').value;

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (response.ok && data.token) {
            if (data.role !== 'ROLE_ADMIN') {
                showAlert('Access Denied: This portal requires administrator privileges.', 'error');
                return;
            }
            sessionStorage.setItem('bank_token', data.token);
            sessionStorage.setItem('bank_user', JSON.stringify(data));
            window.location.href = '/admin.html';
        } else {
            showAlert(data.error || 'Administrator login failed.', 'error');
        }
    } catch (err) {
        showAlert('Network error: ' + err.message, 'error');
    }
}
