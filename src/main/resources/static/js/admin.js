// ==========================================================================
// NEXUS CORE - Enterprise Cluster Supervisor Console Controller
// ==========================================================================

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
let currentBranches = [];
let pendingModalAction = null;
let refreshIntervalMs = 5000;

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('adminName').innerText = adminUser.fullName || adminUser.username;
    loadAdminDashboard();
    startAutoRefresh();
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
    } else if (type === 'warning') {
        iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`;
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
    if (amount === undefined || amount === null) return '₹ 0.00';
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    }).format(amount);
}

function switchAdminTab(tabId, btn) {
    document.querySelectorAll('.admin-nav-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

    if (btn) btn.classList.add('active');
    const target = document.getElementById(tabId);
    if (target) target.classList.add('active');
}

function startAutoRefresh() {
    if (autoRefreshInterval) clearInterval(autoRefreshInterval);
    if (refreshIntervalMs > 0) {
        autoRefreshInterval = setInterval(() => {
            loadAdminDashboard(true);
        }, refreshIntervalMs);
    }
}

function changeAutoRefreshInterval() {
    const select = document.getElementById('autoRefreshIntervalSelect');
    refreshIntervalMs = parseInt(select.value, 10);
    startAutoRefresh();
    if (refreshIntervalMs > 0) {
        showToast(`Auto-refresh interval set to ${refreshIntervalMs / 1000}s.`, 'info');
    } else {
        showToast('Auto-refresh paused.', 'warning');
    }
}

// Modal Confirmation Dialog Controller
function openConfirmModal(title, message, isDanger, onConfirm) {
    const modal = document.getElementById('confirmModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalBody = document.getElementById('modalBody');
    const confirmBtn = document.getElementById('modalConfirmBtn');
    const modalIcon = document.getElementById('modalIcon');

    modalTitle.innerText = title;
    modalBody.innerHTML = message;

    if (isDanger) {
        modalIcon.className = 'modal-icon-box danger';
        confirmBtn.className = 'btn btn-primary';
        confirmBtn.style.background = 'linear-gradient(135deg, #e11d48, #be123c)';
        confirmBtn.style.borderColor = '#fb7185';
    } else {
        modalIcon.className = 'modal-icon-box warning';
        confirmBtn.className = 'btn btn-primary';
        confirmBtn.style.background = '';
        confirmBtn.style.borderColor = '';
    }

    pendingModalAction = onConfirm;
    confirmBtn.onclick = () => {
        if (pendingModalAction) pendingModalAction();
        closeConfirmModal();
    };

    modal.classList.add('show');
}

function closeConfirmModal() {
    const modal = document.getElementById('confirmModal');
    modal.classList.remove('show');
    pendingModalAction = null;
}

// Master Dashboard Data Loader
async function loadAdminDashboard(isBackground = false) {
    const refreshBtn = document.getElementById('btnManualRefresh');
    if (!isBackground && refreshBtn) {
        refreshBtn.disabled = true;
    }

    try {
        const response = await fetch('/api/admin/dashboard', {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.status === 401 || response.status === 403) {
            logoutAdmin();
            return;
        }

        const data = await response.json();
        currentBranches = data.branches || [];
        renderDashboardData(data);
        validateElectionInitiator();
    } catch (err) {
        if (!isBackground) {
            showAlert('Failed to connect to cluster supervisor: ' + err.message, 'error');
            showToast('Cluster communication error: ' + err.message, 'error');
        }
    } finally {
        if (!isBackground && refreshBtn) {
            refreshBtn.disabled = false;
        }
    }
}

function renderDashboardData(data) {
    // 1. KPI Metric Ribbon
    const activeNodesEl = document.getElementById('statActiveNodes');
    const activeNodesSubEl = document.getElementById('statActiveNodesSub');
    const navClusterStatus = document.getElementById('navClusterStatus');

    if (activeNodesEl) {
        activeNodesEl.innerText = `${data.onlineBranches} / ${data.totalBranches} Active`;
    }
    if (activeNodesSubEl) {
        activeNodesSubEl.innerText = data.onlineBranches === data.totalBranches
            ? 'All Nodes Healthy & Synced'
            : 'Fault Tolerant Mode: Partial Outage Active';
    }
    if (navClusterStatus) {
        navClusterStatus.innerText = data.onlineBranches === data.totalBranches
            ? 'CLUSTER OPERATIONAL'
            : 'DEGRADED CLUSTER - FAULT ACTIVE';
        navClusterStatus.style.color = data.onlineBranches === data.totalBranches ? '#34d399' : '#fbbf24';
    }

    const totalAccountsEl = document.getElementById('statTotalAccounts');
    const totalBalanceEl = document.getElementById('statTotalBalance');
    if (totalAccountsEl) totalAccountsEl.innerText = `Across ${data.totalAccounts || 0} Bank Accounts`;
    if (totalBalanceEl) totalBalanceEl.innerText = formatCurrency(data.totalSystemBalance);

    const coordEl = document.getElementById('statCoordinator');
    const coordRoleEl = document.getElementById('statCoordinatorRole');
    if (coordEl) {
        if (data.activeCoordinatorId) {
            coordEl.innerText = `Node ${data.activeCoordinatorId}`;
            if (coordRoleEl) coordRoleEl.innerText = 'Bully Elected 2PC Coordinator';
        } else {
            coordEl.innerText = 'UNAVAILABLE';
            if (coordRoleEl) coordRoleEl.innerText = 'Coordinator Down - Election Required';
        }
    }

    const clockEl = document.getElementById('statLamportClock');
    const navClockEl = document.getElementById('navClockTick');
    if (clockEl) clockEl.innerText = `L = ${data.currentLamportClock}`;
    if (navClockEl) navClockEl.innerText = `L = ${data.currentLamportClock}`;

    // 2. Node Topology Cards
    renderTopologyGrid(data.branches || []);

    // 3. Update Election Initiator Options
    updateElectionDropdownOptions(data.branches || []);

    // 4. Two-Phase Commit Table
    renderTwoPhaseTable(data.twoPhaseTransactions || []);

    // 5. Lamport Event Stream
    renderLamportEvents(data.lamportEvents || []);

    // 6. Replication Records Table
    if (data.replicationRecords) {
        renderReplicationRecords(data.replicationRecords);
    } else {
        loadReplicationRecords();
    }

    // 7. Master Accounts Table
    renderAccountsTable(data.accounts || []);
}

function renderTopologyGrid(branches) {
    const topologyGrid = document.getElementById('topologyGrid');
    if (!topologyGrid) return;

    topologyGrid.innerHTML = branches.map(b => {
        const state = (b.status || 'UNKNOWN').toUpperCase();
        const isRunning = state === 'RUNNING' || state === 'ONLINE';
        const isStopped = state === 'STOPPED';
        const isCrashed = state === 'CRASHED';
        const isRecovering = state === 'RECOVERING';
        const isCoord = Boolean(b.isCoordinator);

        let cardClass = 'node-card';
        if (isCoord) cardClass += ' is-coordinator';
        if (isCrashed) cardClass += ' is-crashed';
        if (isStopped) cardClass += ' is-stopped';

        let badgeClass = 'badge-stopped';
        if (isRunning) badgeClass = 'badge-running';
        else if (isCrashed) badgeClass = 'badge-crashed';
        else if (isRecovering) badgeClass = 'badge-recovering';

        return `
            <div class="${cardClass}">
                <div>
                    <div class="node-header">
                        <div class="node-title-group">
                            <span class="node-title">${b.branchName}</span>
                            <span class="node-subtitle">Node ${b.id} • ${b.branchCode}</span>
                        </div>
                        <div class="node-badges">
                            ${isCoord ? '<span class="status-badge badge-coordinator">Coordinator Head</span>' : ''}
                            <span class="status-badge ${badgeClass}">${state}</span>
                        </div>
                    </div>

                    <div class="node-meta-list">
                        <div class="node-meta-item">
                            <span class="node-meta-label">RMI Registry Endpoint:</span>
                            <span class="node-meta-val mono" style="color: #38bdf8;">${b.rmiHost}:${b.rmiPort}/${b.rmiServiceName}</span>
                        </div>
                        <div class="node-meta-item">
                            <span class="node-meta-label">Geographic Location:</span>
                            <span class="node-meta-val">${b.location}</span>
                        </div>
                        <div class="node-meta-item">
                            <span class="node-meta-label">Cluster Consensus Role:</span>
                            <span class="node-meta-val" style="color: ${isCoord ? '#38bdf8' : 'var(--text-secondary)'}; font-weight: ${isCoord ? '700' : '500'};">
                                ${isCoord ? 'Cluster Coordinator (2PC Head)' : 'Participant Branch Node'}
                            </span>
                        </div>
                    </div>
                </div>

                <div class="node-controls">
                    ${isRunning ? `
                        <button onclick="confirmStopNode(${b.id}, '${b.branchName}')" class="btn btn-outline btn-sm">
                            Stop Gracefully
                        </button>
                        <button onclick="confirmCrashNode(${b.id}, '${b.branchName}')" class="btn btn-danger-outline btn-sm">
                            Simulate Crash
                        </button>
                    ` : `
                        <button onclick="startOrRecoverNode(${b.id}, '${b.branchName}')" class="btn btn-success-outline btn-sm">
                            Start / Recover Node
                        </button>
                    `}
                    <button onclick="triggerBullyFromNode(${b.id})" class="btn btn-outline btn-sm" ${!isRunning ? 'disabled title="Unavailable node cannot initiate election"' : ''}>
                        Elect from Here
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function updateElectionDropdownOptions(branches) {
    const select = document.getElementById('electionInitiator');
    if (!select) return;

    const currentVal = select.value;
    const newOptionsHtml = branches.map(b => {
        const state = (b.status || 'UNKNOWN').toUpperCase();
        return `<option value="${b.id}">${b.branchName} (Node ${b.id}) [${state}]</option>`;
    }).join('');

    if (select.innerHTML !== newOptionsHtml) {
        select.innerHTML = newOptionsHtml;
        if (currentVal && branches.some(b => String(b.id) === String(currentVal))) {
            select.value = currentVal;
        }
    }
}

// Frontend Validation on Election Initiator
function validateElectionInitiator() {
    const select = document.getElementById('electionInitiator');
    const runBtn = document.getElementById('btnRunElection');
    const helperAlert = document.getElementById('electionHelperAlert');
    const helperText = document.getElementById('electionHelperText');

    if (!select || !runBtn) return;

    const selectedId = LongOrString(select.value);
    const selectedBranch = currentBranches.find(b => String(b.id) === String(selectedId));

    if (!selectedBranch) return;

    const state = (selectedBranch.status || 'UNKNOWN').toUpperCase();
    const isEligible = state === 'RUNNING' || state === 'ONLINE';

    if (!isEligible) {
        runBtn.disabled = true;
        if (helperAlert && helperText) {
            helperAlert.className = state === 'CRASHED' ? 'alert alert-error' : 'alert alert-warning';
            helperText.innerText = `Election cannot be initiated: Node ${selectedBranch.id} (${selectedBranch.branchName}) is ${state}. Only active, healthy nodes in the RUNNING state can initiate consensus.`;
            helperAlert.style.display = 'flex';
        }
    } else {
        runBtn.disabled = false;
        if (helperAlert) {
            helperAlert.style.display = 'none';
        }
    }
}

function LongOrString(val) {
    return isNaN(val) ? val : Number(val);
}

// Node Lifecycle Actions with Modal Confirmation
function confirmStopNode(nodeId, nodeName) {
    openConfirmModal(
        `Gracefully Stop Node ${nodeId}?`,
        `Are you sure you want to stop <strong>Node ${nodeId} (${nodeName})</strong>? This will set its state to <code>STOPPED</code>, revoke its coordinator role if active, and exclude it from election participation.`,
        false,
        () => executeNodeStateTransition(nodeId, 'STOPPED')
    );
}

function confirmCrashNode(nodeId, nodeName) {
    openConfirmModal(
        `Simulate Abrupt Crash for Node ${nodeId}?`,
        `Are you sure you want to simulate an abrupt power or network partition crash for <strong>Node ${nodeId} (${nodeName})</strong>? This will trigger fault-tolerance failover routines.`,
        true,
        () => executeNodeStateTransition(nodeId, 'CRASHED')
    );
}

function startOrRecoverNode(nodeId, nodeName) {
    executeNodeRecovery(nodeId, nodeName);
}

async function executeNodeStateTransition(nodeId, state) {
    try {
        const resp = await fetch(`/api/distributed/node/${nodeId}/state?state=${state}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await resp.json();

        if (resp.ok && data.success) {
            showToast(data.message, state === 'CRASHED' ? 'error' : 'warning');
            loadAdminDashboard();
        } else {
            showToast(data.error || 'Failed to update node state.', 'error');
        }
    } catch (e) {
        showToast('Communication error: ' + e.message, 'error');
    }
}

async function executeNodeRecovery(nodeId, nodeName) {
    try {
        const resp = await fetch(`/api/distributed/node/${nodeId}/recover`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await resp.json();

        if (resp.ok && data.success) {
            showToast(`Node ${nodeId} recovered successfully! Reconciled records: ${data.reconciledRecords}`, 'success');
            loadAdminDashboard();
        } else {
            showToast(data.error || 'Recovery failed.', 'error');
        }
    } catch (e) {
        showToast('Recovery error: ' + e.message, 'error');
    }
}

function triggerBullyFromNode(nodeId) {
    const select = document.getElementById('electionInitiator');
    if (select) {
        select.value = nodeId;
        validateElectionInitiator();
        const runBtn = document.getElementById('btnRunElection');
        if (runBtn && !runBtn.disabled) {
            runBullyElection();
        }
    }
}

// Bully Leader Election Runner
async function runBullyElection() {
    clearAlert();
    const select = document.getElementById('electionInitiator');
    const runBtn = document.getElementById('btnRunElection');
    const initiatorId = select.value;
    const container = document.getElementById('electionTraceContainer');

    runBtn.disabled = true;
    const originalText = runBtn.innerHTML;
    runBtn.innerHTML = `
        <svg class="spinner" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="2" x2="12" y2="6"></line><line x1="12" y1="18" x2="12" y2="22"></line>
            <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
            <line x1="2" y1="12" x2="6" y2="12"></line><line x1="18" y1="12" x2="22" y2="12"></line>
            <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
        </svg>
        Conducting Election...
    `;

    container.innerHTML = `
        <div style="color: var(--text-secondary); font-size: 0.85rem; padding: 0.5rem;">
            Initiating Bully Leader Election from Node ${initiatorId}... Dispatching ELECTION messages to higher-ID nodes.
        </div>
    `;

    try {
        const response = await fetch(`/api/distributed/election/start?initiatorId=${initiatorId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const data = await response.json();

        if (response.ok && data.success && data.electedLeaderId) {
            showToast(`Bully Election Concluded: Node ${data.electedLeaderId} elected Coordinator.`, 'success');

            const steps = data.electionSteps || [];
            container.innerHTML = steps.map(step => {
                const isWin = step.eventType === 'COORDINATOR_ANNOUNCED' || step.eventType === 'COORDINATOR_UPDATED';
                const isExcluded = step.eventType === 'NODE_EXCLUDED';
                const isTakeover = step.eventType === 'OK_RECEIVED';

                let lineClass = 'election-step-line';
                if (isWin) lineClass += ' win';
                else if (isExcluded) lineClass += ' excluded';
                else if (isTakeover) lineClass += ' takeover';

                return `
                    <div class="${lineClass}">
                        <div class="election-step-header">
                            <span>[${step.eventType}] Node ${step.nodeId}</span>
                            <span class="mono" style="color: #c084fc; font-size: 0.75rem;">L(e) = ${step.timestamp}</span>
                        </div>
                        <div style="color: var(--text-secondary); font-size: 0.8rem;">${step.details}</div>
                    </div>
                `;
            }).join('');

            loadAdminDashboard();
        } else {
            const errorMsg = data.error || 'Bully election was rejected by backend consensus rules.';
            showToast(errorMsg, 'error');
            showAlert(errorMsg, 'error');

            container.innerHTML = `
                <div class="election-step-line excluded">
                    <div class="election-step-header">
                        <span style="color: #fb7185;">[ELECTION_REJECTED] Node ${initiatorId}</span>
                    </div>
                    <div style="color: #fb7185; font-size: 0.8rem;">${errorMsg}</div>
                </div>
            `;
            loadAdminDashboard();
        }
    } catch (err) {
        showToast('Election transport error: ' + err.message, 'error');
        container.innerHTML = `<div style="color: #fb7185; font-size: 0.85rem; padding: 0.5rem;">Error: ${err.message}</div>`;
    } finally {
        runBtn.innerHTML = originalText;
        validateElectionInitiator();
    }
}

// 2PC Transactions Table Renderer
function renderTwoPhaseTable(transactions) {
    const tbody = document.getElementById('twoPhaseTableBody');
    if (!tbody) return;

    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2rem;">No cross-branch 2PC transactions logged yet.</td></tr>';
        return;
    }

    tbody.innerHTML = transactions.map(gtx => {
        const srcVoteClass = gtx.phase1SourceVote === 'VOTE_COMMIT' ? 'vote-commit' : (gtx.phase1SourceVote === 'VOTE_ABORT' ? 'vote-abort' : 'vote-pending');
        const dstVoteClass = gtx.phase1DestVote === 'VOTE_COMMIT' ? 'vote-commit' : (gtx.phase1DestVote === 'VOTE_ABORT' ? 'vote-abort' : 'vote-pending');
        const isCommitted = gtx.state === 'COMMITTED';
        const dateStr = gtx.createdAt ? gtx.createdAt.replace('T', ' ').substring(0, 19) : '-';

        return `
            <tr>
                <td class="mono" style="font-weight: 700; color: #ffffff;">${gtx.globalTransactionId}</td>
                <td class="mono" style="font-size: 0.775rem; color: var(--text-muted);">${dateStr}</td>
                <td class="mono" style="font-size: 0.825rem;">${gtx.sourceAccount} (B${gtx.sourceBranchId}) → ${gtx.destAccount} (B${gtx.destBranchId})</td>
                <td class="align-right mono" style="font-weight: 700;">${formatCurrency(gtx.amount)}</td>
                <td><span class="vote-badge ${srcVoteClass}">B${gtx.sourceBranchId}: ${gtx.phase1SourceVote}</span></td>
                <td><span class="vote-badge ${dstVoteClass}">B${gtx.destBranchId}: ${gtx.phase1DestVote}</span></td>
                <td class="mono" style="font-size: 0.785rem; font-weight: 700;">${gtx.decision || 'IN_PROGRESS'}</td>
                <td>
                    <span class="status-badge ${isCommitted ? 'badge-running' : 'badge-crashed'}" style="font-size: 0.68rem;">
                        ${gtx.state}
                    </span>
                </td>
                <td class="mono" style="color: #c084fc;">L = ${gtx.lamportTimestamp}</td>
            </tr>
        `;
    }).join('');
}

// Replication Records Loader
async function loadReplicationRecords() {
    try {
        const resp = await fetch('/api/admin/replication-records', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const records = await resp.json();
        renderReplicationRecords(records);
    } catch (e) {
        console.error('Error fetching replication records:', e);
    }
}

function renderReplicationRecords(records) {
    const tbody = document.getElementById('replicationTableBody');
    if (!tbody) return;

    if (!records || records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2rem;">No replica audit records found.</td></tr>';
        return;
    }

    tbody.innerHTML = records.slice(0, 25).map(r => {
        const isSynced = r.syncStatus === 'SYNCED';
        const dateStr = r.syncedAt ? r.syncedAt.replace('T', ' ').substring(0, 19) : '-';

        return `
            <tr>
                <td class="mono" style="font-weight: 700;">#${r.sequenceNumber}</td>
                <td class="mono" style="font-size: 0.775rem; color: var(--text-muted);">${dateStr}</td>
                <td>Branch ${r.sourceBranchId}</td>
                <td>Branch ${r.targetBranchId}</td>
                <td class="mono" style="font-size: 0.775rem;">${r.transactionRef}</td>
                <td class="mono">${r.accountNumber}</td>
                <td class="align-right mono" style="font-weight: 700;">${formatCurrency(r.amount)}</td>
                <td>
                    <span class="status-badge ${isSynced ? 'badge-running' : 'badge-crashed'}" style="font-size: 0.68rem;">
                        ${r.syncStatus}
                    </span>
                </td>
                <td class="mono" style="color: #c084fc;">L = ${r.lamportTimestamp}</td>
            </tr>
        `;
    }).join('');
}

// Lamport Events Table Renderer
function renderLamportEvents(events) {
    const tbody = document.getElementById('lamportTableBody');
    if (!tbody) return;

    if (!events || events.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 2rem;">No Lamport events logged.</td></tr>';
        return;
    }

    tbody.innerHTML = events.slice(0, 30).map(ev => {
        const dateStr = ev.createdAt ? ev.createdAt.replace('T', ' ').substring(0, 19) : '-';
        return `
            <tr>
                <td class="mono" style="font-weight: 800; color: #c084fc; font-size: 0.9rem;">L = ${ev.logicalTimestamp}</td>
                <td style="font-weight: 600; font-size: 0.825rem;">Node ${ev.nodeId} (${ev.nodeName})</td>
                <td><span class="badge-protocol" style="font-size: 0.72rem;">${ev.eventType}</span></td>
                <td style="font-size: 0.825rem;">${ev.message}</td>
                <td class="mono" style="font-size: 0.775rem; color: var(--text-muted);">${dateStr}</td>
            </tr>
        `;
    }).join('');
}

// Master Accounts Table Renderer
function renderAccountsTable(accounts) {
    const tbody = document.getElementById('accountsTableBody');
    if (!tbody) return;

    if (!accounts || accounts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 2rem;">No accounts loaded.</td></tr>';
        return;
    }

    tbody.innerHTML = accounts.map(a => `
        <tr>
            <td class="mono" style="font-weight: 700; color: #ffffff;">${a.accountNumber}</td>
            <td style="font-weight: 600;">${a.user ? a.user.fullName : '-'}</td>
            <td>${a.branch ? a.branch.branchName : '-'}</td>
            <td><span class="badge-protocol">${a.accountType}</span></td>
            <td class="align-right mono" style="font-weight: 800; color: #34d399;">${formatCurrency(a.balance)}</td>
            <td><span class="status-badge badge-running" style="font-size: 0.68rem;">${a.status}</span></td>
        </tr>
    `).join('');
}

async function reconcileBranch(branchId) {
    try {
        const response = await fetch(`/api/distributed/replication/reconcile?branchId=${branchId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await response.json();
        if (response.ok && data.success) {
            showToast(`Branch ${branchId} reconciled ${data.reconciledRecords} records successfully.`, 'success');
            loadAdminDashboard();
        } else {
            showToast(data.error || 'Reconciliation failed.', 'error');
        }
    } catch (err) {
        showToast('Reconciliation transport error: ' + err.message, 'error');
    }
}
