// Administrator Dashboard Controller

const token = sessionStorage.getItem('bank_token');
const userStr = sessionStorage.getItem('bank_user');

if (!token || !userStr) {
    window.location.href = '/index.html';
}

const adminUser = JSON.parse(userStr);
if (adminUser.role !== 'ROLE_ADMIN') {
    alert('Access Denied: Administrator privileges required.');
    window.location.href = '/index.html';
}

let autoRefreshInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('adminName').innerText = adminUser.fullName || adminUser.username;
    loadAdminDashboard();
    startAutoRefresh();
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

function logoutAdmin() {
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

function startAutoRefresh() {
    if (autoRefreshInterval) clearInterval(autoRefreshInterval);
    autoRefreshInterval = setInterval(() => {
        loadAdminDashboard(true);
    }, 5000);
}

function stopAutoRefresh() {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
        autoRefreshInterval = null;
    }
}

function toggleAutoRefresh() {
    const cb = document.getElementById('autoRefreshCheckbox');
    if (cb.checked) {
        startAutoRefresh();
    } else {
        stopAutoRefresh();
    }
}

async function loadAdminDashboard(isBackground = false) {
    try {
        const response = await fetch('/api/admin/dashboard', {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.status === 401 || response.status === 403) {
            logoutAdmin();
            return;
        }

        const data = await response.json();
        renderDashboardData(data);
    } catch (err) {
        if (!isBackground) {
            showAlert('Failed to connect to cluster supervisor: ' + err.message, 'error');
        }
    }
}

function renderDashboardData(data) {
    // 1. Stats
    document.getElementById('statActiveNodes').innerText = `${data.onlineBranches} / ${data.totalBranches}`;
    document.getElementById('statActiveNodesSub').innerText = data.onlineBranches === data.totalBranches ? 'All Nodes Healthy' : 'Partial Outage Detected';
    document.getElementById('statTotalAccounts').innerText = data.totalAccounts;
    document.getElementById('statTotalBalance').innerText = `${formatCurrency(data.totalSystemBalance)} Total Deposits`;
    document.getElementById('statCoordinator').innerText = `Node ${data.activeCoordinatorId}`;
    document.getElementById('statLamportClock').innerText = `L = ${data.currentLamportClock}`;

    // 2. Topology Grid
    const topologyGrid = document.getElementById('topologyGrid');
    if (data.branches) {
        topologyGrid.innerHTML = data.branches.map(b => {
            const isOnline = b.status === 'ONLINE';
            const isCoord = b.isCoordinator;
            const badgeClass = isOnline ? 'badge-online' : 'badge-offline';

            return `
                <div class="node-card ${isCoord ? 'coordinator' : ''} ${!isOnline ? 'offline' : ''}">
                    <div class="node-header">
                        <div>
                            <div class="node-name">${b.branchName}</div>
                            <div class="node-id mono">Node ID: ${b.id} • ${b.branchCode}</div>
                        </div>
                        <span class="badge ${badgeClass}">${b.status}</span>
                    </div>

                    <div class="node-meta">
                        <div>Endpoint: <span class="mono">${b.rmiHost}:${b.rmiPort}/${b.rmiServiceName}</span></div>
                        <div>Location: <span>${b.location}</span></div>
                        <div>Role: <strong style="color: ${isCoord ? 'var(--accent-blue)' : 'var(--text-muted)'};">${isCoord ? '★ 2PC Coordinator / Leader' : 'Participant Branch Node'}</strong></div>
                    </div>

                    <div class="node-actions">
                        ${isOnline
                            ? `<button onclick="simulateNodeFailure(${b.id}, true)" class="btn btn-danger btn-sm">Simulate Crash / Stop</button>`
                            : `<button onclick="simulateNodeFailure(${b.id}, false)" class="btn btn-primary btn-sm">Recover Node</button>`
                        }
                        <button onclick="triggerBullyFromNode(${b.id})" class="btn btn-outline btn-sm">Elect from Here</button>
                    </div>
                </div>
            `;
        }).join('');
    }

    // 3. 2PC Transactions Monitor
    const twoPhaseBody = document.getElementById('twoPhaseTableBody');
    if (data.twoPhaseTransactions && data.twoPhaseTransactions.length > 0) {
        twoPhaseBody.innerHTML = data.twoPhaseTransactions.map(gtx => {
            const srcVoteClass = gtx.phase1SourceVote === 'VOTE_COMMIT' ? 'vote-commit' : (gtx.phase1SourceVote === 'VOTE_ABORT' ? 'vote-abort' : 'vote-pending');
            const dstVoteClass = gtx.phase1DestVote === 'VOTE_COMMIT' ? 'vote-commit' : (gtx.phase1DestVote === 'VOTE_ABORT' ? 'vote-abort' : 'vote-pending');
            const stateClass = gtx.state === 'COMMITTED' ? 'badge-success' : 'badge-danger';
            const dateStr = gtx.createdAt ? gtx.createdAt.replace('T', ' ').substring(0, 19) : '-';

            return `
                <tr>
                    <td class="mono" style="font-weight: 700; color: var(--primary-navy);">${gtx.globalTransactionId}</td>
                    <td style="font-size: 0.8rem; color: var(--text-muted);">${dateStr}</td>
                    <td class="mono" style="font-size: 0.8rem;">${gtx.sourceAccount} (B${gtx.sourceBranchId}) → ${gtx.destAccount} (B${gtx.destBranchId})</td>
                    <td class="mono" style="font-weight: 700;">${formatCurrency(gtx.amount)}</td>
                    <td><span class="vote-badge ${srcVoteClass}">B${gtx.sourceBranchId}: ${gtx.phase1SourceVote}</span></td>
                    <td><span class="vote-badge ${dstVoteClass}">B${gtx.destBranchId}: ${gtx.phase1DestVote}</span></td>
                    <td class="mono" style="font-size: 0.8rem; font-weight: 600;">${gtx.decision || 'IN_PROGRESS'}</td>
                    <td><span class="badge ${stateClass}">${gtx.state}</span></td>
                    <td class="mono" style="color: var(--text-muted);">${gtx.lamportTimestamp}</td>
                </tr>
            `;
        }).join('');
    } else {
        twoPhaseBody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No cross-branch 2PC transactions logged yet.</td></tr>';
    }

    // 4. Lamport Event Stream
    const lamportBody = document.getElementById('lamportTableBody');
    if (data.lamportEvents && data.lamportEvents.length > 0) {
        lamportBody.innerHTML = data.lamportEvents.slice(0, 30).map(ev => {
            const dateStr = ev.createdAt ? ev.createdAt.replace('T', ' ').substring(0, 19) : '-';
            return `
                <tr>
                    <td class="mono" style="font-weight: 700; color: var(--status-online); font-size: 0.95rem;">L = ${ev.logicalTimestamp}</td>
                    <td style="font-weight: 600; font-size: 0.8rem;">Node ${ev.nodeId} (${ev.nodeName})</td>
                    <td><span class="badge badge-primary mono" style="font-size: 0.72rem;">${ev.eventType}</span></td>
                    <td style="font-size: 0.825rem;">${ev.message}</td>
                    <td style="font-size: 0.78rem; color: var(--text-muted);">${dateStr}</td>
                </tr>
            `;
        }).join('');
    } else {
        lamportBody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No Lamport events logged.</td></tr>';
    }

    // 5. Replication Table
    loadReplicationRecords();

    // 6. Accounts Table
    const accountsBody = document.getElementById('accountsTableBody');
    if (data.accounts) {
        accountsBody.innerHTML = data.accounts.map(a => `
            <tr>
                <td class="mono" style="font-weight: 700;">${a.accountNumber}</td>
                <td>${a.user ? a.user.fullName : '-'}</td>
                <td>${a.branch ? a.branch.branchName : '-'}</td>
                <td><span class="badge badge-primary">${a.accountType}</span></td>
                <td class="mono" style="font-weight: 700;">${formatCurrency(a.balance)}</td>
                <td><span class="badge badge-online">${a.status}</span></td>
            </tr>
        `).join('');
    }
}

async function loadReplicationRecords() {
    try {
        const resp = await fetch('/api/admin/replication-records', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const records = await resp.json();
        const tbody = document.getElementById('replicationTableBody');

        if (!records || records.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No replica audit records found.</td></tr>';
            return;
        }

        tbody.innerHTML = records.slice(0, 20).map(r => {
            const statusClass = r.syncStatus === 'SYNCED' ? 'badge-success' : 'badge-danger';
            const dateStr = r.syncedAt ? r.syncedAt.replace('T', ' ').substring(0, 19) : '-';

            return `
                <tr>
                    <td class="mono" style="font-weight: 600;">#${r.sequenceNumber}</td>
                    <td style="font-size: 0.8rem; color: var(--text-muted);">${dateStr}</td>
                    <td>Branch ${r.sourceBranchId}</td>
                    <td>Branch ${r.targetBranchId}</td>
                    <td class="mono" style="font-size: 0.78rem;">${r.transactionRef}</td>
                    <td class="mono">${r.accountNumber}</td>
                    <td class="mono" style="font-weight: 600;">${formatCurrency(r.amount)}</td>
                    <td><span class="badge ${statusClass}">${r.syncStatus}</span></td>
                    <td class="mono" style="color: var(--text-muted);">${r.lamportTimestamp}</td>
                </tr>
            `;
        }).join('');
    } catch (e) {
        console.error('Error fetching replication records:', e);
    }
}

async function simulateNodeFailure(nodeId, shouldFail) {
    clearAlert();
    try {
        const response = await fetch(`/api/distributed/node/${nodeId}/simulate-failure?fail=${shouldFail}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await response.json();
        if (response.ok && data.success) {
            showAlert(data.message, shouldFail ? 'error' : 'success');
            loadAdminDashboard();
        } else {
            showAlert(data.error || 'Failed to update node status.', 'error');
        }
    } catch (err) {
        showAlert('Communication error: ' + err.message, 'error');
    }
}

function triggerBullyFromNode(nodeId) {
    document.getElementById('electionInitiator').value = nodeId;
    runBullyElection();
}

async function runBullyElection() {
    clearAlert();
    const initiatorId = document.getElementById('electionInitiator').value;
    const container = document.getElementById('electionTraceContainer');
    container.innerHTML = `<div style="color: var(--text-muted); font-size: 0.85rem;">Initiating Bully algorithm from Node ${initiatorId}...</div>`;

    try {
        const response = await fetch(`/api/distributed/election/start?initiatorId=${initiatorId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const data = await response.json();

        if (response.ok && data.electedLeaderId) {
            showAlert(`Bully election completed! New Coordinator elected: Node ${data.electedLeaderId}`, 'success');

            container.innerHTML = data.electionSteps.map(step => {
                const isWin = step.eventType === 'COORDINATOR_ANNOUNCED';
                return `
                    <div class="election-step ${isWin ? 'win' : ''}">
                        <div style="display: flex; justify-content: space-between;">
                            <strong>[${step.eventType}] Node ${step.nodeId}</strong>
                            <span class="mono" style="color: var(--text-muted); font-size: 0.75rem;">L = ${step.timestamp}</span>
                        </div>
                        <div style="margin-top: 0.2rem;">${step.details}</div>
                    </div>
                `;
            }).join('');

            loadAdminDashboard();
        } else {
            showAlert(data.error || 'Bully election failed.', 'error');
        }
    } catch (err) {
        showAlert('Election error: ' + err.message, 'error');
    }
}

async function reconcileBranch(branchId) {
    clearAlert();
    try {
        const response = await fetch(`/api/distributed/replication/reconcile?branchId=${branchId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await response.json();
        if (response.ok && data.success) {
            showAlert(`Branch ${branchId} reconciliation completed! Reconciled records: ${data.reconciledRecords}`, 'success');
            loadAdminDashboard();
        } else {
            showAlert(data.error || 'Reconciliation failed.', 'error');
        }
    } catch (err) {
        showAlert('Reconciliation error: ' + err.message, 'error');
    }
}
