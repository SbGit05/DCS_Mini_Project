#!/usr/bin/env python3
"""
Generates a high-resolution, publication-grade Architecture Diagram
for the Distributed Banking System.
"""

import matplotlib.pyplot as plt
import matplotlib.patches as patches

def create_architecture_diagram(output_path="docs/images/our_system_architecture.png"):
    fig, ax = plt.subplots(figsize=(14, 10.5), dpi=300)
    ax.set_xlim(0, 14)
    ax.set_ylim(0, 10.5)
    ax.axis('off')

    # Background canvas
    canvas_bg = patches.Rectangle((0, 0), 14, 10.5, facecolor="#f8fafc", edgecolor="#cbd5e1", linewidth=1.5)
    ax.add_patch(canvas_bg)

    # Master Container
    master_box = patches.FancyBboxPatch((0.4, 0.4), 13.2, 9.7, boxstyle="round,pad=0.1,rounding_size=0.15",
                                        facecolor="#ffffff", edgecolor="#1e293b", linewidth=2.0)
    ax.add_patch(master_box)

    # Title Banner
    title_box = patches.FancyBboxPatch((0.8, 9.3), 12.4, 0.65, boxstyle="round,pad=0.08,rounding_size=0.1",
                                       facecolor="#0f172a", edgecolor="#0284c7", linewidth=1.5)
    ax.add_patch(title_box)
    ax.text(7.0, 9.62, "DISTRIBUTED BANKING SYSTEM — MULTI-TIER RMI ARCHITECTURE",
            ha="center", va="center", color="#ffffff", fontsize=13.5, weight="bold", fontfamily="sans-serif")

    # -------------------------------------------------------------
    # TIER 1: CLIENT PRESENTATION LAYER
    # -------------------------------------------------------------
    t1_box = patches.FancyBboxPatch((0.8, 7.65), 12.4, 1.45, boxstyle="round,pad=0.08,rounding_size=0.12",
                                    facecolor="#f0f9ff", edgecolor="#0284c7", linewidth=1.6)
    ax.add_patch(t1_box)
    ax.text(1.1, 8.85, "TIER 1: CLIENT PRESENTATION LAYER (HTML5 / CSS3 / JavaScript Portals)",
            ha="left", va="center", color="#0369a1", fontsize=10.5, weight="bold")

    # Client Portal Cards
    c1 = patches.FancyBboxPatch((1.0, 7.8), 3.6, 0.9, boxstyle="round,pad=0.06,rounding_size=0.08",
                               facecolor="#ffffff", edgecolor="#38bdf8", linewidth=1.2)
    ax.add_patch(c1)
    ax.text(2.8, 8.4, "Customer Banking Portal", ha="center", va="center", color="#0f172a", fontsize=9.5, weight="bold")
    ax.text(2.8, 8.05, "Balance Inquiry • Cash Deposit • Withdrawal\nCross-Branch Fund Transfer • Passbook Ledger",
            ha="center", va="center", color="#475569", fontsize=7.5)

    c2 = patches.FancyBboxPatch((4.9, 7.8), 4.2, 0.9, boxstyle="round,pad=0.06,rounding_size=0.08",
                               facecolor="#ffffff", edgecolor="#38bdf8", linewidth=1.2)
    ax.add_patch(c2)
    ax.text(7.0, 8.4, "Cluster Admin Operations Console", ha="center", va="center", color="#0f172a", fontsize=9.5, weight="bold")
    ax.text(7.0, 8.05, "Live Topology Grid • 2PC Monitor • Replication Stream\nBully Leader Election Studio • Fault Injection",
            ha="center", va="center", color="#475569", fontsize=7.5)

    c3 = patches.FancyBboxPatch((9.4, 7.8), 3.6, 0.9, boxstyle="round,pad=0.06,rounding_size=0.08",
                               facecolor="#ffffff", edgecolor="#38bdf8", linewidth=1.2)
    ax.add_patch(c3)
    ax.text(11.2, 8.4, "Authentication & Role Gateway", ha="center", va="center", color="#0f172a", fontsize=9.5, weight="bold")
    ax.text(11.2, 8.05, "BCrypt Password Encryption ($2a$10$)\nUUID Session Token Validation • Role Auth",
            ha="center", va="center", color="#475569", fontsize=7.5)

    # Arrow Tier 1 -> Tier 2
    ax.annotate("", xy=(7.0, 6.75), xytext=(7.0, 7.65),
                arrowprops=dict(arrowstyle="-|>", color="#0284c7", lw=2, mutation_scale=15))
    ax.text(7.15, 7.2, "HTTP / REST JSON Endpoints (Port 8085)", ha="left", va="center", color="#0369a1", fontsize=8.5, weight="bold")

    # -------------------------------------------------------------
    # TIER 2: APPLICATION GATEWAY & CONTROLLER LAYER
    # -------------------------------------------------------------
    t2_box = patches.FancyBboxPatch((0.8, 5.7), 12.4, 1.05, boxstyle="round,pad=0.08,rounding_size=0.12",
                                    facecolor="#f8fafc", edgecolor="#475569", linewidth=1.6)
    ax.add_patch(t2_box)
    ax.text(1.1, 6.55, "TIER 2: APPLICATION GATEWAY & REST CONTROLLERS (Spring Boot)",
            ha="left", va="center", color="#334155", fontsize=10.5, weight="bold")

    ctrl_w = 2.85
    ctrls = [
        (1.0, "AuthController", "Login, Register, Logout\nBCrypt Password Check"),
        (4.1, "CustomerController", "Deposit, Withdraw, Transfer\nPassbook & Balance API"),
        (7.2, "AdminController", "Cluster Topology, Node Status\nFault Injection Triggers"),
        (10.3, "DistributedController", "2PC Transaction Routing\nBully Election Trigger API")
    ]
    for x, name, desc in ctrls:
        cb = patches.FancyBboxPatch((x, 5.82), ctrl_w, 0.65, boxstyle="round,pad=0.05,rounding_size=0.08",
                                    facecolor="#ffffff", edgecolor="#94a3b8", linewidth=1.1)
        ax.add_patch(cb)
        ax.text(x + ctrl_w/2, 6.25, name, ha="center", va="center", color="#0f172a", fontsize=8.5, weight="bold")
        ax.text(x + ctrl_w/2, 5.98, desc, ha="center", va="center", color="#475569", fontsize=7.0)

    # Arrow Tier 2 -> Tier 3
    ax.annotate("", xy=(7.0, 4.75), xytext=(7.0, 5.7),
                arrowprops=dict(arrowstyle="-|>", color="#0f766e", lw=2, mutation_scale=15))
    ax.text(7.15, 5.22, "Java RMI Remote Method Invocation (Port 1099 Registry)", ha="left", va="center", color="#0f766e", fontsize=8.5, weight="bold")

    # -------------------------------------------------------------
    # TIER 3: DISTRIBUTED RMI SERVICE & COORDINATION CLUSTER
    # -------------------------------------------------------------
    t3_box = patches.FancyBboxPatch((0.8, 2.3), 12.4, 2.45, boxstyle="round,pad=0.08,rounding_size=0.12",
                                    facecolor="#f0fdf4", edgecolor="#16a34a", linewidth=1.6)
    ax.add_patch(t3_box)
    ax.text(1.1, 4.55, "TIER 3: DISTRIBUTED RMI SERVICE & COORDINATION LAYER (Multi-Branch Cluster)",
            ha="left", va="center", color="#15803d", fontsize=10.5, weight="bold")

    # Branch Nodes
    b_w = 3.8
    # Node 1: Pune
    bn1 = patches.FancyBboxPatch((1.0, 3.2), b_w, 1.15, boxstyle="round,pad=0.06,rounding_size=0.08",
                                facecolor="#ffffff", edgecolor="#22c55e", linewidth=1.3)
    ax.add_patch(bn1)
    ax.text(1.0 + b_w/2, 4.15, "Branch Node 1: Pune Central", ha="center", va="center", color="#14532d", fontsize=9, weight="bold")
    ax.text(1.0 + b_w/2, 3.88, "Service: BranchService_1 (Port 1099)\nAuthoritative Accounts: PUN-1001, PUN-1002\nState: RUNNING • RMI Stub Bound",
            ha="center", va="center", color="#334155", fontsize=7.5)
    ax.text(1.0 + b_w/2, 3.35, "Role: 2PC Participant • Bully Participant", ha="center", va="center", color="#15803d", fontsize=7.0, weight="bold")

    # Node 2: Mumbai
    bn2 = patches.FancyBboxPatch((5.1, 3.2), b_w, 1.15, boxstyle="round,pad=0.06,rounding_size=0.08",
                                facecolor="#ffffff", edgecolor="#22c55e", linewidth=1.3)
    ax.add_patch(bn2)
    ax.text(5.1 + b_w/2, 4.15, "Branch Node 2: Mumbai Main", ha="center", va="center", color="#14532d", fontsize=9, weight="bold")
    ax.text(5.1 + b_w/2, 3.88, "Service: BranchService_2 (Port 1099)\nAuthoritative Accounts: MUM-2001, MUM-2002\nState: RUNNING • RMI Stub Bound",
            ha="center", va="center", color="#334155", fontsize=7.5)
    ax.text(5.1 + b_w/2, 3.35, "Role: 2PC Participant • Bully Backup", ha="center", va="center", color="#15803d", fontsize=7.0, weight="bold")

    # Node 3: Delhi
    bn3 = patches.FancyBboxPatch((9.2, 3.2), b_w, 1.15, boxstyle="round,pad=0.06,rounding_size=0.08",
                                facecolor="#eff6ff", edgecolor="#2563eb", linewidth=1.5)
    ax.add_patch(bn3)
    ax.text(9.2 + b_w/2, 4.15, "Branch Node 3: Delhi Regional Hub", ha="center", va="center", color="#1e3a8a", fontsize=9, weight="bold")
    ax.text(9.2 + b_w/2, 3.88, "Service: BranchService_3 (Port 1099)\nAuthoritative Accounts: DEL-3001, DEL-3002\nState: RUNNING • Highest Node ID",
            ha="center", va="center", color="#334155", fontsize=7.5)
    ax.text(9.2 + b_w/2, 3.35, "★ ACTIVE 2PC COORDINATOR (Bully Leader) ★", ha="center", va="center", color="#1d4ed8", fontsize=7.0, weight="bold")

    # Coordination Sub-Boxes
    coord1 = patches.FancyBboxPatch((1.0, 2.45), 2.85, 0.65, boxstyle="round,pad=0.04,rounding_size=0.06",
                                   facecolor="#fef3c7", edgecolor="#d97706", linewidth=1.1)
    ax.add_patch(coord1)
    ax.text(2.42, 2.88, "Two-Phase Commit (2PC)", ha="center", va="center", color="#92400e", fontsize=8, weight="bold")
    ax.text(2.42, 2.62, "Prepare (Vote) → Commit / Abort\nPrepared Reservations Hold", ha="center", va="center", color="#78350f", fontsize=6.8)

    coord2 = patches.FancyBboxPatch((4.05, 2.45), 2.9, 0.65, boxstyle="round,pad=0.04,rounding_size=0.06",
                                   facecolor="#fef3c7", edgecolor="#d97706", linewidth=1.1)
    ax.add_patch(coord2)
    ax.text(5.5, 2.88, "Bully Leader Election", ha="center", va="center", color="#92400e", fontsize=8, weight="bold")
    ax.text(5.5, 2.62, "ELECTION → OK → COORDINATOR\nStrict Node Liveness Validation", ha="center", va="center", color="#78350f", fontsize=6.8)

    coord3 = patches.FancyBboxPatch((7.15, 2.45), 2.9, 0.65, boxstyle="round,pad=0.04,rounding_size=0.06",
                                   facecolor="#fef3c7", edgecolor="#d97706", linewidth=1.1)
    ax.add_patch(coord3)
    ax.text(8.6, 2.88, "Lamport Logical Clocks", ha="center", va="center", color="#92400e", fontsize=8, weight="bold")
    ax.text(8.6, 2.62, "L(e) Causal Partial Ordering\nMonotonic Clocks on Messages", ha="center", va="center", color="#78350f", fontsize=6.8)

    coord4 = patches.FancyBboxPatch((10.25, 2.45), 2.75, 0.65, boxstyle="round,pad=0.04,rounding_size=0.06",
                                   facecolor="#fef3c7", edgecolor="#d97706", linewidth=1.1)
    ax.add_patch(coord4)
    ax.text(11.62, 2.88, "Heartbeat & Replication", ha="center", va="center", color="#92400e", fontsize=8, weight="bold")
    ax.text(11.62, 2.62, "5s Node Heartbeat Ping/Pong\nAsync Audit Reconciliation", ha="center", va="center", color="#78350f", fontsize=6.8)

    # Arrow Tier 3 -> Tier 4
    ax.annotate("", xy=(7.0, 1.7), xytext=(7.0, 2.3),
                arrowprops=dict(arrowstyle="-|>", color="#9333ea", lw=2, mutation_scale=15))
    ax.text(7.15, 2.0, "Spring Data JPA / HikariCP JDBC Connection Pool", ha="left", va="center", color="#7e22ce", fontsize=8.5, weight="bold")

    # -------------------------------------------------------------
    # TIER 4: PERSISTENCE LAYER (MySQL InnoDB Database)
    # -------------------------------------------------------------
    t4_box = patches.FancyBboxPatch((0.8, 0.6), 12.4, 1.1, boxstyle="round,pad=0.08,rounding_size=0.12",
                                    facecolor="#faf5ff", edgecolor="#9333ea", linewidth=1.6)
    ax.add_patch(t4_box)
    ax.text(1.1, 1.5, "TIER 4: RELATIONAL PERSISTENCE LAYER (MySQL 8.0+ InnoDB Engine — ACID Compliant)",
            ha="left", va="center", color="#6b21a8", fontsize=10.5, weight="bold")

    tbl_w = 2.85
    tbls = [
        (1.0, "accounts & users", "PESSIMISTIC_WRITE Row Locks\nSELECT ... FOR UPDATE\nBCrypt Passwords & Roles"),
        (4.1, "transactions & log", "Idempotency Deduplication\nAuthoritative Transaction Ledger\nLamport Logical Timestamps"),
        (7.2, "two_phase_transactions", "Global Transaction IDs (GTX)\nPrepare / Commit State Machine\nParticipant Vote Audit Records"),
        (10.3, "prepared_reservations", "Temporary Fund Holds\nDouble-Spending Prevention\nReplication Audit Stream")
    ]
    for x, name, desc in tbls:
        tb = patches.FancyBboxPatch((x, 0.72), tbl_w, 0.68, boxstyle="round,pad=0.05,rounding_size=0.08",
                                    facecolor="#ffffff", edgecolor="#d8b4fe", linewidth=1.1)
        ax.add_patch(tb)
        ax.text(x + tbl_w/2, 1.22, name, ha="center", va="center", color="#0f172a", fontsize=8.5, weight="bold")
        ax.text(x + tbl_w/2, 0.93, desc, ha="center", va="center", color="#475569", fontsize=6.8)

    plt.tight_layout()
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"[OK] Generated Architecture Diagram at: {output_path}")

if __name__ == "__main__":
    create_architecture_diagram("docs/images/our_system_architecture.png")
    create_architecture_diagram("docs/images/architecture_diagram.png")
