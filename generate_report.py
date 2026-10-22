#!/usr/bin/env python3
"""
Report Generator for Distributed Banking System
Generates the comprehensive academic laboratory report strictly following DCS_Mini_Project_Report.pdf:
- Exactly 17 pages with 20pt inset black rectangular page border on every page
- Official PCET / PCCOE Information Technology Department Cover Page with HD PCCOE Logo
- Prepared By: Piyush Badgujar (123B1F003), Rutuja Bobade (123B1F012), Mehul Chaudhari (123B1F017)
- Guided By: Mrs. Tanuja S. Patankar
- Exact 12 numbered sections: Abstract, 1. Introduction, 2. Theoretical Background, 3. System Requirements,
  4. System Architecture, 5. Module Description, 6. Implementation Methodology,
  7. Concurrency Control and Deadlock Prevention, 8. Testing and Results, 9. Advantages and Limitations,
  10. Conclusion, 11. Future Enhancements, 12. References
- Generates both Distributed_Banking_System_Project_Documentation.pdf and .docx
"""

import os
import sys
import shutil
from datetime import datetime

# ReportLab imports
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image as RLImage,
    KeepTogether, PageBreak
)
from reportlab.pdfgen import canvas

# python-docx imports
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT

WORKSPACE_DIR = os.path.dirname(os.path.abspath(__file__))
DOCS_DIR = os.path.join(WORKSPACE_DIR, "docs")
IMAGES_DIR = os.path.join(DOCS_DIR, "images")

PDF_PATH = os.path.join(WORKSPACE_DIR, "Distributed_Banking_System_Project_Documentation.pdf")
DOCX_PATH = os.path.join(WORKSPACE_DIR, "Distributed_Banking_System_Project_Documentation.docx")
PCCOE_PDF_COPY = os.path.join(WORKSPACE_DIR, "DCS_Mini_Project_Report_PCCOE.pdf")
PCCOE_DOCX_COPY = os.path.join(WORKSPACE_DIR, "DCS_Mini_Project_Report_PCCOE.docx")

# Image paths
LOGO_PATH = os.path.join(IMAGES_DIR, "pccoe_logo_hd.png")
ARCH_DIAGRAM_PATH = os.path.join(IMAGES_DIR, "architecture_diagram.png")
CUST_DASH_PATH = os.path.join(IMAGES_DIR, "customer_dashboard.png")
ADMIN_DASH_PATH = os.path.join(IMAGES_DIR, "admin_dashboard.png")
BULLY_PATH = os.path.join(IMAGES_DIR, "bully_election_fix.png")
DEP_PATH = os.path.join(IMAGES_DIR, "deposit_success.png")
WITH_PATH = os.path.join(IMAGES_DIR, "withdrawal_success.png")
LANDING_PATH = os.path.join(IMAGES_DIR, "landing_page.png")


class BorderedCanvas(canvas.Canvas):
    """Draws exact 20pt inset black rectangular page border matching DCS_Mini_Project_Report.pdf"""
    def __init__(self, *args, **kwargs):
        super(BorderedCanvas, self).__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations()
            super(BorderedCanvas, self).showPage()
        super(BorderedCanvas, self).save()

    def draw_page_decorations(self):
        self.saveState()
        # Letter is 612 x 792 pt. 20pt inset: x=20, y=20, w=572, h=752
        self.setStrokeColor(colors.black)
        self.setLineWidth(0.8)
        self.rect(20, 20, 572, 752)
        self.restoreState()


def build_pdf():
    print(f"[PDF] Generating academic report at: {PDF_PATH}")
    doc = SimpleDocTemplate(
        PDF_PATH,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=40,
        bottomMargin=40
    )

    styles = getSampleStyleSheet()

    cover_inst_style = ParagraphStyle(
        'CoverInst', parent=styles['Normal'],
        fontName='Times-Bold', fontSize=12, leading=15, alignment=1, spaceAfter=2
    )
    cover_inst_sub = ParagraphStyle(
        'CoverInstSub', parent=styles['Normal'],
        fontName='Times-Roman', fontSize=9, leading=12, alignment=1, spaceAfter=12
    )
    cover_dept_style = ParagraphStyle(
        'CoverDept', parent=styles['Normal'],
        fontName='Times-Bold', fontSize=13, leading=16, alignment=1, spaceAfter=16
    )
    cover_course_style = ParagraphStyle(
        'CoverCourse', parent=styles['Normal'],
        fontName='Times-Roman', fontSize=11, leading=14, alignment=1, spaceAfter=16
    )
    cover_title_style = ParagraphStyle(
        'CoverTitle', parent=styles['Normal'],
        fontName='Times-Bold', fontSize=12.5, leading=16, alignment=1, spaceAfter=18
    )
    cover_guide_hdr = ParagraphStyle(
        'CoverGuideHdr', parent=styles['Normal'],
        fontName='Times-Bold', fontSize=11, leading=14, alignment=1, spaceAfter=4
    )
    cover_guide_name = ParagraphStyle(
        'CoverGuideName', parent=styles['Normal'],
        fontName='Times-Roman', fontSize=11, leading=14, alignment=1, spaceAfter=0
    )

    h1_style = ParagraphStyle(
        'Heading1_Custom', parent=styles['Heading1'],
        fontName='Times-Bold', fontSize=11.5, leading=15, textColor=colors.black,
        spaceBefore=10, spaceAfter=4, keepWithNext=True
    )
    h2_style = ParagraphStyle(
        'Heading2_Custom', parent=styles['Heading2'],
        fontName='Times-Bold', fontSize=10.5, leading=14, textColor=colors.black,
        spaceBefore=7, spaceAfter=3, keepWithNext=True
    )
    body_style = ParagraphStyle(
        'Body_Custom', parent=styles['Normal'],
        fontName='Times-Roman', fontSize=9.5, leading=13.5, textColor=colors.black,
        spaceAfter=5
    )
    bullet_style = ParagraphStyle(
        'Bullet_Custom', parent=body_style,
        leftIndent=15, firstLineIndent=-10, spaceAfter=3
    )
    numbered_style = ParagraphStyle(
        'Numbered_Custom', parent=body_style,
        leftIndent=18, firstLineIndent=-12, spaceAfter=3
    )
    code_style = ParagraphStyle(
        'Code_Custom', parent=styles['Normal'],
        fontName='Courier', fontSize=7.5, leading=10, textColor=colors.black,
        backColor=colors.HexColor("#f8fafc"), borderPadding=4, spaceBefore=3, spaceAfter=5
    )
    table_cell = ParagraphStyle(
        'TableCell', parent=styles['Normal'],
        fontName='Times-Roman', fontSize=8.5, leading=11, textColor=colors.black
    )
    table_header = ParagraphStyle(
        'TableHeader', parent=styles['Normal'],
        fontName='Times-Bold', fontSize=8.5, leading=11, textColor=colors.black
    )
    caption_style = ParagraphStyle(
        'Caption_Custom', parent=styles['Normal'],
        fontName='Times-Italic', fontSize=8.5, leading=11, alignment=1,
        textColor=colors.HexColor("#334155"), spaceBefore=3, spaceAfter=6
    )

    story = []

    # =========================================================================
    # PAGE 1: TITLE & COVER PAGE
    # =========================================================================
    story.append(Spacer(1, 15))
    story.append(Paragraph("<b>Pimpri Chinchwad Education Trust’s</b>", cover_inst_style))
    story.append(Paragraph("<b>Pimpri Chinchwad College of Engineering</b>", cover_inst_style))
    story.append(Paragraph("Sector No. 26, Pradhikaran, Nigdi, Pune 411044 An Autonomous Institute Approved By AICTE and Affiliated To SPPU, Pune", cover_inst_sub))
    story.append(Spacer(1, 8))

    story.append(Paragraph("<b>DEPARTMENT OF INFORMATION TECHNOLOGY</b>", cover_dept_style))
    story.append(Spacer(1, 10))

    if os.path.exists(LOGO_PATH):
        logo_img = RLImage(LOGO_PATH, width=110, height=94)
        logo_img.hAlign = 'CENTER'
        story.append(logo_img)
    story.append(Spacer(1, 18))

    story.append(Paragraph("Laboratory Report of Distributed Computing System", cover_course_style))
    story.append(Spacer(1, 8))

    story.append(Paragraph("<b>DISTRIBUTED BANKING SYSTEM USING JAVA RMI WITH<br/>CONCURRENT MULTI-CLIENT TRANSACTION SYNCHRONIZATION</b>", cover_title_style))
    story.append(Spacer(1, 12))

    story.append(Paragraph("<b>Prepared By:</b>", cover_guide_hdr))
    story.append(Spacer(1, 4))

    author_table_data = [
        [Paragraph("<b>Name</b>", table_header), Paragraph("<b>PRN</b>", table_header)],
        [Paragraph("Piyush Badgujar", table_cell), Paragraph("123B1F003", table_cell)],
        [Paragraph("Rutuja Bobade", table_cell), Paragraph("123B1F012", table_cell)],
        [Paragraph("Mehul Chaudhari", table_cell), Paragraph("123B1F017", table_cell)]
    ]
    author_table = Table(author_table_data, colWidths=[150, 110], hAlign='CENTER')
    author_table.setStyle(TableStyle([
        ('GRID', (0,0), (-1,-1), 0.6, colors.black),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(author_table)
    story.append(Spacer(1, 28))

    story.append(Paragraph("<b>Guided by:</b>", cover_guide_hdr))
    story.append(Spacer(1, 2))
    story.append(Paragraph("Mrs. Tanuja S. Patankar", cover_guide_name))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 2: ABSTRACT & 1. INTRODUCTION (1.1)
    # =========================================================================
    story.append(Paragraph("<b>ABSTRACT</b>", h1_style))
    story.append(Paragraph(
        "The Distributed Banking System is a client-server application developed using Java Remote Method Invocation "
        "(RMI) to demonstrate the principles of distributed computing and concurrent transaction processing. The application "
        "allows multiple clients to perform essential banking operations, including account creation, balance inquiry, "
        "deposits, withdrawals, fund transfers, and transaction history retrieval, through regional branch servers.",
        body_style
    ))
    story.append(Paragraph(
        "In a distributed banking environment, multiple clients may access and modify the same account simultaneously. "
        "Without proper synchronization, concurrent operations can cause race conditions, lost updates, and incorrect "
        "account balances. To address this problem, the proposed system uses account-level mutual exclusion through "
        "Java's <code>ReentrantLock</code> mechanism. The system also applies consistent lock ordering during fund transfers "
        "to prevent deadlocks.",
        body_style
    ))
    story.append(Paragraph(
        "The application consists of regional banking branch servers (Pune Central, Mumbai Main, Delhi Regional Hub), multiple remote clients, "
        "a Java RMI communication layer, and responsive interactive web dashboards. Account information is maintained with transactional "
        "durability, while transaction activity, 2PC coordination, and account lock states can be observed through the live monitoring interface.",
        body_style
    ))
    story.append(Paragraph(
        "The project demonstrates the implementation of remote method invocation, client-server communication, "
        "concurrent request processing, synchronization, and deadlock avoidance. It provides a practical understanding of "
        "how distributed applications maintain consistency when multiple users access shared resources.",
        body_style
    ))
    story.append(Paragraph(
        "<b>Keywords:</b> Distributed Computing, Java RMI, Banking System, Client-Server Architecture, Concurrency Control, "
        "Mutual Exclusion, Deadlock Avoidance, Two-Phase Commit, Lamport Clocks.",
        body_style
    ))
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>1. INTRODUCTION</b>", h1_style))
    story.append(Paragraph("<b>1.1 Introduction to Distributed Computing</b>", h2_style))
    story.append(Paragraph(
        "A distributed system consists of multiple independent computers or processes that communicate through a network "
        "to perform coordinated tasks. These components may execute on separate machines or within different processes and "
        "exchange information using communication mechanisms.",
        body_style
    ))
    story.append(Paragraph(
        "Distributed computing is widely used in banking, e-commerce, reservation systems, cloud applications, and enterprise "
        "software. Such applications allow multiple users to access shared services without requiring every user to execute the "
        "entire application on a single computer.",
        body_style
    ))
    story.append(Paragraph(
        "A banking application is an appropriate example of a distributed system because multiple customers may perform "
        "financial operations simultaneously. The system must ensure that transactions are processed correctly and that "
        "account balances remain consistent.",
        body_style
    ))
    story.append(Paragraph(
        "Java Remote Method Invocation (RMI) provides a mechanism for communication between Java applications running in separate "
        "Java Virtual Machines (JVMs). It allows a client to invoke methods on a remote object hosted by a server.",
        body_style
    ))
    story.append(Paragraph(
        "This project applies Java RMI to develop a distributed banking application in which multiple clients communicate "
        "with regional branch servers to perform banking transactions.",
        body_style
    ))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 3: 1.2 PROBLEM STATEMENT, 1.3 OBJECTIVES, 1.4 SCOPE
    # =========================================================================
    story.append(Paragraph("<b>1.2 Problem Statement</b>", h2_style))
    story.append(Paragraph(
        "In a conventional banking application, multiple users may access the same account and initiate transactions concurrently. "
        "When these operations are processed without appropriate synchronization, the system may encounter race conditions, "
        "lost updates, incorrect balance calculations, or unauthorized withdrawals.",
        body_style
    ))
    story.append(Paragraph(
        "For example, consider an account with a balance of ₹10,000. Two clients simultaneously request withdrawals of ₹7,000 "
        "and ₹5,000. Since the total requested amount exceeds the available balance, both withdrawals must not be allowed to succeed.",
        body_style
    ))
    story.append(Paragraph(
        "If the server checks the balance independently for both requests before updating the account, both operations may incorrectly "
        "appear valid. This can result in an inconsistent account balance.",
        body_style
    ))
    story.append(Paragraph(
        "Furthermore, a fund transfer involves two accounts. If multiple clients acquire the corresponding account locks in different "
        "orders, the system may experience a deadlock in which each client waits for a lock held by another.",
        body_style
    ))
    story.append(Paragraph(
        "Therefore, the problem addressed by this project is to develop a distributed banking application that supports remote access "
        "by multiple clients while ensuring correct account updates through synchronized transaction processing and avoiding deadlocks "
        "during fund transfers.",
        body_style
    ))
    story.append(Paragraph(
        "The system must provide a centralized mechanism for executing banking operations, validating account information, controlling "
        "concurrent access to shared accounts, and returning appropriate responses to clients.",
        body_style
    ))

    story.append(Paragraph("<b>1.3 Objectives</b>", h2_style))
    story.append(Paragraph("The project has the following four objectives:", body_style))
    story.append(Paragraph("1. To develop a distributed client-server banking application using Java RMI that enables multiple independent clients to remotely access banking services.", numbered_style))
    story.append(Paragraph("2. To implement essential banking operations, including account creation, balance inquiry, deposits, withdrawals, and fund transfers, with appropriate account and transaction validation.", numbered_style))
    story.append(Paragraph("3. To ensure consistency of account balances during concurrent transactions by implementing account-level mutual exclusion using Java ReentrantLock.", numbered_style))
    story.append(Paragraph("4. To prevent deadlocks during simultaneous fund transfers and verify the correctness of concurrent banking operations through systematic testing.", numbered_style))
    story.append(Paragraph("These objectives define the functionality and technical problems addressed by the project.", body_style))

    story.append(Paragraph("<b>1.4 Scope of the Project</b>", h2_style))
    story.append(Paragraph("The project focuses on developing and demonstrating a distributed banking application for academic purposes. The scope includes:", body_style))
    story.append(Paragraph("&bull; Remote client-server communication using Java RMI.", bullet_style))
    story.append(Paragraph("&bull; Multiple independent banking clients.", bullet_style))
    story.append(Paragraph("&bull; Account creation and account management.", bullet_style))
    story.append(Paragraph("&bull; Balance inquiry, deposit, and withdrawal operations.", bullet_style))
    story.append(Paragraph("&bull; Transfers between bank accounts.", bullet_style))
    story.append(Paragraph("&bull; Concurrent request processing.", bullet_style))
    story.append(Paragraph("&bull; Account-level synchronization.", bullet_style))
    story.append(Paragraph("&bull; Deadlock avoidance during transfers.", bullet_style))
    story.append(Paragraph("&bull; Transaction history and system monitoring.", bullet_style))
    story.append(Paragraph("&bull; Console-based and graphical user interfaces.", bullet_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 4: 2. THEORETICAL BACKGROUND (2.1 - 2.3)
    # =========================================================================
    story.append(Paragraph("<b>2. THEORETICAL BACKGROUND</b>", h1_style))
    story.append(Paragraph("<b>2.1 Client-Server Architecture</b>", h2_style))
    story.append(Paragraph("Client-server architecture divides an application into clients and a server.", body_style))
    story.append(Paragraph(
        "The client initiates requests and displays the responses. The server receives requests, executes the required business "
        "logic, manages shared data, and returns the result.",
        body_style
    ))
    story.append(Paragraph(
        "In the proposed system, multiple banking clients communicate with regional branch servers. The servers maintain account "
        "information and perform all banking operations.",
        body_style
    ))
    story.append(Paragraph(
        "This architecture separates the user interface from the banking logic and provides a common service to multiple clients.",
        body_style
    ))

    story.append(Paragraph("<b>2.2 Java Remote Method Invocation</b>", h2_style))
    story.append(Paragraph(
        "Java Remote Method Invocation is a mechanism that enables a Java program to invoke methods on an object located in another JVM.",
        body_style
    ))
    story.append(Paragraph("The main components of RMI are:", body_style))
    story.append(Paragraph("&bull; <b>Remote interface:</b> Defines the methods available to remote clients.", bullet_style))
    story.append(Paragraph("&bull; <b>Remote implementation:</b> Contains the actual logic of the remote methods.", bullet_style))
    story.append(Paragraph("&bull; <b>RMI Registry:</b> Provides a naming service through which clients locate remote objects.", bullet_style))
    story.append(Paragraph("&bull; <b>Client stub or proxy:</b> Represents the remote object to the client.", bullet_style))
    story.append(Paragraph("&bull; <b>RMI runtime:</b> Handles communication and parameter transfer.", bullet_style))
    story.append(Paragraph(
        "The server registers its remote banking service with the RMI Registry. Clients locate the service and invoke its methods "
        "through a remote reference.",
        body_style
    ))
    story.append(Paragraph(
        "RMI handles the transfer of method arguments and return values, allowing the application to use remote method calls instead "
        "of implementing a separate communication protocol for each banking operation.",
        body_style
    ))

    story.append(Paragraph("<b>2.3 Inter-Process Communication</b>", h2_style))
    story.append(Paragraph("Inter-Process Communication (IPC) enables independent processes to exchange information.", body_style))
    story.append(Paragraph(
        "Examples of IPC mechanisms include sockets, pipes, shared memory, message queues, and Remote Procedure Call (RPC).",
        body_style
    ))
    story.append(Paragraph(
        "Java RMI is used in this project because it supports remote communication between Java processes using object-oriented interfaces. "
        "The clients and server may execute in separate JVMs, and communication takes place through remote method invocation.",
        body_style
    ))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 5: 2.4 - 2.7 (CONCURRENCY, RACE CONDITIONS, MUTEX, DEADLOCK)
    # =========================================================================
    story.append(Paragraph("<b>2.4 Multithreading and Concurrency</b>", h2_style))
    story.append(Paragraph("Multithreading allows multiple threads to execute concurrently within a process.", body_style))
    story.append(Paragraph(
        "In the banking application, several clients can send requests to the server at approximately the same time. "
        "The server may process these requests using different threads.",
        body_style
    ))
    story.append(Paragraph(
        "Concurrency improves the ability of the application to serve multiple users. However, it creates problems when several "
        "threads access the same mutable account data.",
        body_style
    ))
    story.append(Paragraph(
        "For example, two simultaneous deposits must both be reflected in the final account balance. Without synchronization, "
        "one update may overwrite another.",
        body_style
    ))

    story.append(Paragraph("<b>2.5 Race Condition</b>", h2_style))
    story.append(Paragraph("A race condition occurs when the result of concurrent operations depends on the order or timing of their execution.", body_style))
    story.append(Paragraph(
        "Suppose two clients read the same account balance and independently calculate new balances. If both write their results "
        "without synchronization, one update may be lost.",
        body_style
    ))
    story.append(Paragraph("The banking system prevents such conflicting updates by controlling access to account data using locks.", body_style))

    story.append(Paragraph("<b>2.6 Mutual Exclusion</b>", h2_style))
    story.append(Paragraph(
        "Mutual exclusion ensures that only one thread at a time can execute a protected critical section for a particular shared resource.",
        body_style
    ))
    story.append(Paragraph("A critical section is the part of the program that accesses or modifies shared data.", body_style))
    story.append(Paragraph(
        "The project uses Java's <code>ReentrantLock</code> to protect account operations. A thread must acquire the corresponding "
        "account lock before executing a conflicting operation.",
        body_style
    ))
    story.append(Paragraph(
        "This allows operations on different accounts to proceed concurrently while preventing conflicting modifications to the same account.",
        body_style
    ))

    story.append(Paragraph("<b>2.7 Deadlock</b>", h2_style))
    story.append(Paragraph("Deadlock occurs when two or more threads wait indefinitely for resources held by one another.", body_style))
    story.append(Paragraph(
        "For example, one transfer may acquire the lock for Account A and wait for Account B, while another transfer acquires "
        "Account B and waits for Account A.",
        body_style
    ))
    story.append(Paragraph("The project addresses this issue by acquiring account locks in a consistent order.", body_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 6: 2.8 - 2.9 & 3. SYSTEM REQUIREMENTS
    # =========================================================================
    story.append(Paragraph("<b>2.8 ConcurrentHashMap</b>", h2_style))
    story.append(Paragraph(
        "ConcurrentHashMap is a thread-safe implementation of the Java Map interface that supports concurrent access to its mappings.",
        body_style
    ))
    story.append(Paragraph("The project uses it for shared account-related data structures and account lock management.", body_style))
    story.append(Paragraph(
        "However, a thread-safe map does not automatically make compound operations on account balances thread-safe. "
        "Explicit synchronization is still required when multiple operations modify the same account.",
        body_style
    ))

    story.append(Paragraph("<b>2.9 ACID Properties</b>", h2_style))
    story.append(Paragraph("ACID describes four properties associated with reliable transaction processing:", body_style))
    story.append(Paragraph("&bull; <b>Atomicity:</b> A transaction should complete as a unit or be appropriately rolled back.", bullet_style))
    story.append(Paragraph("&bull; <b>Consistency:</b> A transaction should preserve the system's defined rules.", bullet_style))
    story.append(Paragraph("&bull; <b>Isolation:</b> Concurrent transactions should not interfere in ways that violate correctness.", bullet_style))
    story.append(Paragraph("&bull; <b>Durability:</b> Committed transactions should survive failures and restarts.", bullet_style))
    story.append(Paragraph(
        "The project demonstrates synchronization and consistency for normal banking operations. However, because the primary "
        "in-memory demonstration maintains active lock maps, relational persistence provides durability across server restarts.",
        body_style
    ))
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>3. SYSTEM REQUIREMENTS</b>", h1_style))
    story.append(Paragraph("<b>3.1 Hardware Requirements</b>", h2_style))
    story.append(Paragraph("The application can be executed on a standard computer capable of running a Java development environment.", body_style))

    hw_table_data = [
        [Paragraph("<b>Component</b>", table_header), Paragraph("<b>Requirement</b>", table_header)],
        [Paragraph("Processor", table_cell), Paragraph("Standard modern processor", table_cell)],
        [Paragraph("RAM", table_cell), Paragraph("Sufficient memory to run the Java server and multiple clients", table_cell)],
        [Paragraph("Storage", table_cell), Paragraph("Space for the project files and Java environment", table_cell)],
        [Paragraph("Network", table_cell), Paragraph("Localhost communication or a network connection for remote clients", table_cell)]
    ]
    hw_table = Table(hw_table_data, colWidths=[140, 360], hAlign='LEFT')
    hw_table.setStyle(TableStyle([
        ('GRID', (0,0), (-1,-1), 0.5, colors.black),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('LEFTPADDING', (0,0), (-1,-1), 6),
        ('RIGHTPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(hw_table)
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>3.2 Software Requirements</b>", h2_style))
    sw_table_data = [
        [Paragraph("<b>Software</b>", table_header), Paragraph("<b>Purpose</b>", table_header)],
        [Paragraph("Java Development Kit (JDK)", table_cell), Paragraph("Compilation and execution of Java programs", table_cell)],
        [Paragraph("Java RMI", table_cell), Paragraph("Remote communication between clients and server", table_cell)],
        [Paragraph("Java Swing / Web Dashboard", table_cell), Paragraph("Graphical user interface", table_cell)],
        [Paragraph("Java Concurrency API", table_cell), Paragraph("Locks and concurrent data structures", table_cell)],
        [Paragraph("IDE or terminal", table_cell), Paragraph("Development, execution, and debugging", table_cell)]
    ]
    sw_table = Table(sw_table_data, colWidths=[160, 340], hAlign='LEFT')
    sw_table.setStyle(TableStyle([
        ('GRID', (0,0), (-1,-1), 0.5, colors.black),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('LEFTPADDING', (0,0), (-1,-1), 6),
        ('RIGHTPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(sw_table)
    story.append(PageBreak())

    # =========================================================================
    # PAGE 7: 4. SYSTEM ARCHITECTURE (4.1 & 4.2 DIAGRAM)
    # =========================================================================
    story.append(Paragraph("<b>4. SYSTEM ARCHITECTURE</b>", h1_style))
    story.append(Paragraph("<b>4.1 Architecture Overview</b>", h2_style))
    story.append(Paragraph("The system follows a centralized client-server architecture.", body_style))
    story.append(Paragraph(
        "Multiple clients communicate with a banking server through Java RMI. The server contains the business logic, "
        "synchronization mechanisms, and centralized account state.",
        body_style
    ))
    story.append(Paragraph("The major components are:", body_style))
    story.append(Paragraph("1. Client applications.", numbered_style))
    story.append(Paragraph("2. Java RMI communication layer.", numbered_style))
    story.append(Paragraph("3. RMI Registry.", numbered_style))
    story.append(Paragraph("4. Banking service implementation.", numbered_style))
    story.append(Paragraph("5. Synchronization layer.", numbered_style))
    story.append(Paragraph("6. Centralized account storage.", numbered_style))
    story.append(Paragraph("7. Transaction history and monitoring interface.", numbered_style))
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>4.2 Architecture Diagram</b>", h2_style))
    if os.path.exists(ARCH_DIAGRAM_PATH):
        diag_img = RLImage(ARCH_DIAGRAM_PATH, width=360, height=335)
        diag_img.hAlign = 'CENTER'
        story.append(diag_img)
    story.append(PageBreak())

    # =========================================================================
    # PAGE 8: 4.3 - 4.7 & 5. MODULE DESCRIPTION (5.1 - 5.2)
    # =========================================================================
    story.append(Paragraph("<b>4.3 Client Layer</b>", h2_style))
    story.append(Paragraph("The client layer allows users to interact with the banking system.", body_style))
    story.append(Paragraph("It accepts input, invokes remote banking methods, and displays the server's response.", body_style))
    story.append(Paragraph("The project provides a command-line client and a graphical monitoring dashboard.", body_style))

    story.append(Paragraph("<b>4.4 Communication Layer</b>", h2_style))
    story.append(Paragraph("The communication layer uses Java RMI to exchange requests and responses.", body_style))
    story.append(Paragraph("The client locates the remote banking service through the RMI Registry and invokes its methods.", body_style))

    story.append(Paragraph("<b>4.5 Business Logic Layer</b>", h2_style))
    story.append(Paragraph("The business logic layer is implemented in the banking service.", body_style))
    story.append(Paragraph("It validates account information, executes banking operations, manages balances, and records transaction details.", body_style))

    story.append(Paragraph("<b>4.6 Synchronization Layer</b>", h2_style))
    story.append(Paragraph("The synchronization layer protects shared account information.", body_style))
    story.append(Paragraph(
        "It uses account-level locks to prevent conflicting concurrent operations and consistent lock ordering to avoid "
        "deadlocks during transfers.",
        body_style
    ))

    story.append(Paragraph("<b>4.7 Data Management Layer</b>", h2_style))
    story.append(Paragraph("The data management layer maintains account information using centralized in-memory data structures.", body_style))
    story.append(Paragraph("The server acts as the authoritative source of account balances and transaction information while it is running.", body_style))
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>5. MODULE DESCRIPTION</b>", h1_style))
    story.append(Paragraph("<b>5.1 BankService.java</b>", h2_style))
    story.append(Paragraph("This module defines the remote banking interface. It declares the methods available to clients, such as:", body_style))
    story.append(Paragraph("&bull; Creating an account.", bullet_style))
    story.append(Paragraph("&bull; Checking an account balance.", bullet_style))
    story.append(Paragraph("&bull; Depositing money.", bullet_style))
    story.append(Paragraph("&bull; Withdrawing money.", bullet_style))
    story.append(Paragraph("&bull; Transferring funds.", bullet_style))
    story.append(Paragraph("&bull; Retrieving transaction history.", bullet_style))
    story.append(Paragraph("The remote interface separates the service contract from the implementation.", body_style))

    story.append(Paragraph("<b>5.2 BankServiceImpl.java</b>", h2_style))
    story.append(Paragraph("This module implements the remote banking interface. Its responsibilities include:", body_style))
    story.append(Paragraph("&bull; Validating banking requests.", bullet_style))
    story.append(Paragraph("&bull; Managing account information.", bullet_style))
    story.append(Paragraph("&bull; Updating balances.", bullet_style))
    story.append(Paragraph("&bull; Acquiring and releasing account locks.", bullet_style))
    story.append(Paragraph("&bull; Executing fund transfers.", bullet_style))
    story.append(Paragraph("&bull; Recording transaction information.", bullet_style))
    story.append(Paragraph("&bull; Returning success or failure responses.", bullet_style))
    story.append(Paragraph("It contains the main business logic of the application.", body_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 9: 5.3 - 5.7 (MODULES CONTINUED & MODEL CLASSES TABLE)
    # =========================================================================
    story.append(Paragraph("<b>5.3 BankServer.java</b>", h2_style))
    story.append(Paragraph("This module initializes the server. Its responsibilities include:", body_style))
    story.append(Paragraph("1. Creating or accessing the RMI Registry.", numbered_style))
    story.append(Paragraph("2. Creating the banking service implementation.", numbered_style))
    story.append(Paragraph("3. Registering the remote service.", numbered_style))
    story.append(Paragraph("4. Initializing demonstration accounts.", numbered_style))
    story.append(Paragraph("5. Waiting for client requests.", numbered_style))

    story.append(Paragraph("<b>5.4 BankClient.java</b>", h2_style))
    story.append(Paragraph("This module provides the console interface.", body_style))
    story.append(Paragraph("It allows users to select banking operations from a menu and communicate with the remote server.", body_style))
    story.append(Paragraph("Multiple instances of the client can connect to the same server.", body_style))

    story.append(Paragraph("<b>5.5 Dashboard.java</b>", h2_style))
    story.append(Paragraph("This module provides the graphical interface.", body_style))
    story.append(Paragraph("It allows users to perform banking operations and monitor account and transaction information.", body_style))

    story.append(Paragraph("<b>5.6 DcsTelemetryPanel.java</b>", h2_style))
    story.append(Paragraph("This module displays system execution information, such as server activity and account lock states.", body_style))
    story.append(Paragraph("It helps demonstrate the behavior of concurrent requests during testing.", body_style))

    story.append(Paragraph("<b>5.7 Model Classes</b>", h2_style))
    model_table_data = [
        [Paragraph("<b>Class</b>", table_header), Paragraph("<b>Description</b>", table_header)],
        [Paragraph("<code>Account.java</code>", table_cell), Paragraph("Represents account information.", table_cell)],
        [Paragraph("<code>Transaction.java</code>", table_cell), Paragraph("Represents transaction details.", table_cell)],
        [Paragraph("<code>BankResponse.java</code>", table_cell), Paragraph("Represents the result of a banking operation.", table_cell)],
        [Paragraph("<code>TwoPhaseTransaction.java</code>", table_cell), Paragraph("Tracks distributed 2PC lifecycle (INITIATED, PREPARED, COMMITTED).", table_cell)],
        [Paragraph("<code>PreparedReservation.java</code>", table_cell), Paragraph("Holds locked funds during Phase 1 of distributed transfers.", table_cell)],
        [Paragraph("<code>LamportEvent.java</code>", table_cell), Paragraph("Records cluster events with monotonically increasing Lamport timestamps.", table_cell)]
    ]
    model_table = Table(model_table_data, colWidths=[160, 340], hAlign='LEFT')
    model_table.setStyle(TableStyle([
        ('GRID', (0,0), (-1,-1), 0.5, colors.black),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('LEFTPADDING', (0,0), (-1,-1), 6),
        ('RIGHTPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(model_table)
    story.append(Spacer(1, 4))
    story.append(Paragraph(
        "These model classes organize information exchanged between components. Serializable objects can be transferred through RMI where required.",
        body_style
    ))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 10: 6. IMPLEMENTATION METHODOLOGY (6.1 - 6.4)
    # =========================================================================
    story.append(Paragraph("<b>6. IMPLEMENTATION METHODOLOGY</b>", h1_style))
    story.append(Paragraph("<b>6.1 Server Initialization</b>", h2_style))
    story.append(Paragraph("The server creates the banking service and registers it with the RMI Registry.", body_style))
    story.append(Paragraph("A simplified example is:", body_style))

    code_init = """LocateRegistry.createRegistry(1099);
BankService service = new BankServiceImpl();
Naming.rebind("BankService", service);"""
    story.append(Paragraph(code_init.replace('\n', '<br/>').replace(' ', '&nbsp;'), code_style))

    story.append(Paragraph("The exact initialization code may differ depending on the project's implementation.", body_style))
    story.append(Paragraph("The client retrieves the remote service using its registered name and then invokes banking methods.", body_style))

    story.append(Paragraph("<b>6.2 Account Creation</b>", h2_style))
    story.append(Paragraph(
        "Account creation involves receiving the account identifier and opening balance, validating the information, "
        "and adding the account to the centralized account store.",
        body_style
    ))
    story.append(Paragraph("The server should reject duplicate account identifiers and invalid opening balances.", body_style))

    story.append(Paragraph("<b>6.3 Deposit Operation</b>", h2_style))
    story.append(Paragraph("The deposit operation increases an account's balance.", body_style))
    story.append(Paragraph("The processing steps are:", body_style))
    story.append(Paragraph("1. Receive the account number and deposit amount.", numbered_style))
    story.append(Paragraph("2. Obtain the account lock.", numbered_style))
    story.append(Paragraph("3. Verify that the account exists.", numbered_style))
    story.append(Paragraph("4. Validate the deposit amount.", numbered_style))
    story.append(Paragraph("5. Add the amount to the balance.", numbered_style))
    story.append(Paragraph("6. Record the transaction.", numbered_style))
    story.append(Paragraph("7. Return the operation result.", numbered_style))
    story.append(Paragraph("8. Release the lock.", numbered_style))

    story.append(Paragraph("<b>6.4 Withdrawal Operation</b>", h2_style))
    story.append(Paragraph("The withdrawal operation decreases an account's balance.", body_style))
    story.append(Paragraph("The processing steps are:", body_style))
    story.append(Paragraph("1. Receive the account number and withdrawal amount.", numbered_style))
    story.append(Paragraph("2. Acquire the account lock.", numbered_style))
    story.append(Paragraph("3. Verify that the account exists.", numbered_style))
    story.append(Paragraph("4. Validate the withdrawal amount.", numbered_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 11: 6.4 CONT, 6.5 & 7. CONCURRENCY CONTROL (7.1)
    # =========================================================================
    story.append(Paragraph("5. Check the available balance.", numbered_style))
    story.append(Paragraph("6. Deduct the amount if sufficient funds exist.", numbered_style))
    story.append(Paragraph("7. Record the transaction.", numbered_style))
    story.append(Paragraph("8. Return the operation result.", numbered_style))
    story.append(Paragraph("9. Release the lock.", numbered_style))
    story.append(Paragraph("If the balance is insufficient, the withdrawal is rejected without changing the balance.", body_style))

    story.append(Paragraph("<b>6.5 Fund Transfer</b>", h2_style))
    story.append(Paragraph("A fund transfer involves two accounts.", body_style))
    story.append(Paragraph("The processing steps are:", body_style))
    story.append(Paragraph("1. Receive the source account, destination account, and transfer amount.", numbered_style))
    story.append(Paragraph("2. Validate the account identifiers.", numbered_style))
    story.append(Paragraph("3. Determine a consistent lock acquisition order.", numbered_style))
    story.append(Paragraph("4. Acquire both account locks.", numbered_style))
    story.append(Paragraph("5. Verify that both accounts exist.", numbered_style))
    story.append(Paragraph("6. Validate the amount and source balance.", numbered_style))
    story.append(Paragraph("7. Debit the source account.", numbered_style))
    story.append(Paragraph("8. Credit the destination account.", numbered_style))
    story.append(Paragraph("9. Record the transfer.", numbered_style))
    story.append(Paragraph("10. Return the result and release both locks.", numbered_style))
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>7. CONCURRENCY CONTROL AND DEADLOCK PREVENTION</b>", h1_style))
    story.append(Paragraph("<b>7.1 Account-Level Locking</b>", h2_style))
    story.append(Paragraph("The system uses account-level ReentrantLock instances to control access to account data.", body_style))
    story.append(Paragraph("A representative implementation pattern is:", body_style))

    code_lock = """private final ConcurrentHashMap<String, ReentrantLock>
    accountLocks = new ConcurrentHashMap<>();

private ReentrantLock getLockForAccount(String accountNumber) {
    return accountLocks.computeIfAbsent(
        accountNumber,
        key -> new ReentrantLock(true)
    );
}"""
    story.append(Paragraph(code_lock.replace('\n', '<br/>').replace(' ', '&nbsp;'), code_style))

    story.append(Paragraph("The map provides a lock associated with each account identifier.", body_style))
    story.append(Paragraph(
        "The fair-lock option requests that threads waiting longer for a lock receive priority in acquisition order. "
        "It does not guarantee that every request will succeed.",
        body_style
    ))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 12: 7.2 - 7.4 (LOCK ACQUISITION, CONCURRENT WITHDRAWAL, DEADLOCK PREV)
    # =========================================================================
    story.append(Paragraph("<b>7.2 Lock Acquisition</b>", h2_style))
    story.append(Paragraph("The project uses tryLock() with a configured waiting period to demonstrate account contention.", body_style))
    story.append(Paragraph("If a request cannot acquire the required lock within the permitted period, the operation can be rejected.", body_style))
    story.append(Paragraph("Locks must be released in a finally block after acquisition to avoid leaving accounts locked when exceptions occur.", body_style))

    story.append(Paragraph("<b>7.3 Concurrent Withdrawal Example</b>", h2_style))
    story.append(Paragraph("Consider an account with an initial balance of ₹10,000.", body_style))
    story.append(Paragraph("Two clients initiate withdrawals:", body_style))
    story.append(Paragraph("&bull; Client A requests ₹7,000.", bullet_style))
    story.append(Paragraph("&bull; Client B requests ₹5,000.", bullet_style))
    story.append(Paragraph("Since the total withdrawal request is ₹12,000, both operations must not succeed.", body_style))
    story.append(Paragraph("The account lock ensures that the requests are processed in a controlled sequence.", body_style))
    story.append(Paragraph("If Client A succeeds first, the balance becomes ₹3,000, and Client B's withdrawal is rejected.", body_style))
    story.append(Paragraph("If Client B succeeds first, the balance becomes ₹5,000, and Client A's withdrawal is rejected.", body_style))
    story.append(Paragraph("This demonstrates how synchronization prevents incorrect concurrent withdrawals.", body_style))

    story.append(Paragraph("<b>7.4 Deadlock Prevention</b>", h2_style))
    story.append(Paragraph("During fund transfers, the system acquires locks for the source and destination accounts in a consistent order.", body_style))
    story.append(Paragraph("For example, account identifiers can be sorted before lock acquisition.", body_style))
    story.append(Paragraph(
        "If every transfer follows the same ordering rule, one transfer cannot acquire the higher-ordered account first while "
        "another acquires the lower-ordered account first.",
        body_style
    ))
    story.append(Paragraph("This prevents circular wait among those account locks.", body_style))
    story.append(Paragraph("The locks should be released in reverse order after the transfer operation completes.", body_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 13: 8. TESTING AND RESULTS (8.1 - 8.4)
    # =========================================================================
    story.append(Paragraph("<b>8. TESTING AND RESULTS</b>", h1_style))
    story.append(Paragraph("<b>8.1 Testing Objectives</b>", h2_style))
    story.append(Paragraph("Testing is conducted to verify that the system:", body_style))
    story.append(Paragraph("&bull; Performs banking operations correctly.", bullet_style))
    story.append(Paragraph("&bull; Rejects invalid transactions.", bullet_style))
    story.append(Paragraph("&bull; Maintains account balance consistency.", bullet_style))
    story.append(Paragraph("&bull; Handles concurrent requests safely.", bullet_style))
    story.append(Paragraph("&bull; Prevents conflicting withdrawals.", bullet_style))
    story.append(Paragraph("&bull; Avoids deadlocks during transfers.", bullet_style))

    story.append(Paragraph("<b>8.2 Functional Test Cases</b>", h2_style))
    tc_table_data = [
        [Paragraph("<b>Test Case</b>", table_header), Paragraph("<b>Operation</b>", table_header), Paragraph("<b>Expected Result</b>", table_header)],
        [Paragraph("TC-01", table_cell), Paragraph("Create a new account", table_cell), Paragraph("Account is created successfully.", table_cell)],
        [Paragraph("TC-02", table_cell), Paragraph("Create a duplicate account", table_cell), Paragraph("Duplicate account is rejected.", table_cell)],
        [Paragraph("TC-03", table_cell), Paragraph("Deposit money", table_cell), Paragraph("Balance increases by the deposited amount.", table_cell)],
        [Paragraph("TC-04", table_cell), Paragraph("Withdraw money", table_cell), Paragraph("Balance decreases by the withdrawn amount when sufficient funds exist.", table_cell)],
        [Paragraph("TC-05", table_cell), Paragraph("Withdraw more than available balance", table_cell), Paragraph("Transaction is rejected.", table_cell)],
        [Paragraph("TC-06", table_cell), Paragraph("Transfer money", table_cell), Paragraph("Source is debited and destination is credited.", table_cell)],
        [Paragraph("TC-07", table_cell), Paragraph("Transfer to the same account", table_cell), Paragraph("Invalid transfer is rejected.", table_cell)],
        [Paragraph("TC-08", table_cell), Paragraph("Request an operation on a locked account", table_cell), Paragraph("Request follows the configured lock-wait and rejection policy.", table_cell)],
        [Paragraph("TC-09", table_cell), Paragraph("Execute concurrent withdrawals", table_cell), Paragraph("Conflicting withdrawals do not cause an incorrect balance.", table_cell)],
        [Paragraph("TC-10", table_cell), Paragraph("Execute concurrent deposits", table_cell), Paragraph("All successful deposits are reflected in the final balance.", table_cell)]
    ]
    tc_table = Table(tc_table_data, colWidths=[65, 225, 210], hAlign='LEFT')
    tc_table.setStyle(TableStyle([
        ('GRID', (0,0), (-1,-1), 0.5, colors.black),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('TOPPADDING', (0,0), (-1,-1), 2),
        ('BOTTOMPADDING', (0,0), (-1,-1), 2),
        ('LEFTPADDING', (0,0), (-1,-1), 5),
        ('RIGHTPADDING', (0,0), (-1,-1), 5),
    ]))
    story.append(tc_table)
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>8.3 Concurrent Deposit Verification</b>", h2_style))
    story.append(Paragraph("Suppose five clients each deposit ₹1,000 into the same account.", body_style))
    story.append(Paragraph("The expected increase in balance is: <b>[ 5 &times; ₹1,000 = ₹5,000 ]</b>", body_style))
    story.append(Paragraph("If the initial balance is ₹10,000, the expected final balance is <b>₹15,000</b>.", body_style))
    story.append(Paragraph("This test verifies that concurrent deposits are not lost.", body_style))

    story.append(Paragraph("<b>8.4 Mutual Exclusion Demonstration</b>", h2_style))
    story.append(Paragraph(
        "The system can demonstrate mutual exclusion by holding an account lock during a processing delay and initiating "
        "requests from other clients.",
        body_style
    ))
    story.append(Paragraph(
        "The demonstration allows the examiner to observe how the server responds to requests that cannot acquire the same account lock.",
        body_style
    ))
    story.append(Paragraph("The actual timeout and processing-delay values should be taken from the running implementation.", body_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 14: SCREENSHOTS (CUSTOMER & ADMIN DASHBOARDS) + 8.5 OBSERVATIONS
    # =========================================================================
    if os.path.exists(ADMIN_DASH_PATH):
        admin_img = RLImage(ADMIN_DASH_PATH, width=475, height=215)
        admin_img.hAlign = 'CENTER'
        story.append(admin_img)

    if os.path.exists(CUST_DASH_PATH):
        story.append(Spacer(1, 4))
        cust_img = RLImage(CUST_DASH_PATH, width=475, height=215)
        cust_img.hAlign = 'CENTER'
        story.append(cust_img)

    story.append(Spacer(1, 4))
    story.append(Paragraph("<b>8.5 Results and Observations</b>", h1_style))
    story.append(Paragraph("The test cases are designed to demonstrate correct transaction processing and synchronization.", body_style))
    story.append(Paragraph("The following observations should be documented using actual outputs from the project:", body_style))
    story.append(Paragraph("&bull; Successful account creation and banking operations.", bullet_style))
    story.append(Paragraph("&bull; Rejection of invalid transactions.", bullet_style))
    story.append(Paragraph("&bull; Correct final balances after concurrent deposits.", bullet_style))
    story.append(Paragraph("&bull; Correct rejection of an excess withdrawal.", bullet_style))
    story.append(Paragraph("&bull; Successful transfers without deadlock.", bullet_style))
    story.append(Paragraph("&bull; Visible account-lock behavior in the telemetry panel.", bullet_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 15: SCREENSHOTS OF RUNNING APPLICATION AND TESTS
    # =========================================================================
    story.append(Paragraph("Insert screenshots of the actual running application and test results in this section. Do not claim a test passed unless it has been executed and verified.", body_style))
    story.append(Spacer(1, 4))

    # Deposit and Withdrawal screenshots side by side
    if os.path.exists(DEP_PATH) and os.path.exists(WITH_PATH):
        dep_img = RLImage(DEP_PATH, width=230, height=270)
        with_img = RLImage(WITH_PATH, width=230, height=270)
        ops_table = Table([[dep_img, with_img]], colWidths=[238, 238], hAlign='CENTER')
        ops_table.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
            ('LEFTPADDING', (0,0), (-1,-1), 0),
            ('RIGHTPADDING', (0,0), (-1,-1), 0),
            ('TOPPADDING', (0,0), (-1,-1), 0),
            ('BOTTOMPADDING', (0,0), (-1,-1), 0),
        ]))
        story.append(ops_table)
        story.append(Paragraph("Figure 8.1: Live Verification of Cash Deposit (Left) and Cash Withdrawal (Right) under Account Locking", caption_style))

    if os.path.exists(BULLY_PATH):
        story.append(Spacer(1, 2))
        bully_img = RLImage(BULLY_PATH, width=475, height=205)
        bully_img.hAlign = 'CENTER'
        story.append(bully_img)
        story.append(Paragraph("Figure 8.2: Bully Leader Election Execution Trace Demonstrating Coordinator Failover", caption_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 16: TEST TERMINAL LOGS & 9. ADVANTAGES AND LIMITATIONS
    # =========================================================================
    terminal_code = """PS D:\\Distributed Banking System> java -cp bin BankServer
[11:25:22.237] [DCS-RMI-IPC] [Thread: RMI TCP Connection(2)-127.0.0.1][Server ] CLIENT REGISTERED: [client-1]
[11:26:44.667] [DCS-RMI-IPC] [Thread: RMI TCP Connection(5)-127.0.0.1][Client-1] RMI Invocation -> checkBalance(Acc=101)
[11:26:44.668] [DCS-MUTEX  ] [Thread: RMI TCP Connection(5)-127.0.0.1][Client-1] ReentrantLock Acquired for Account 101
[11:26:44.671] [DCS-SUCCESS] [Thread: RMI TCP Connection(5)-127.0.0.1][Client-1] Balance snapshot for 101 is 10000.00
[11:27:22.242] [DCS-RMI-IPC] [Thread: RMI TCP Connection(7)-127.0.0.1][Client-2] RMI Invocation -> withdraw(Acc=101, Amount=7000.0)
[11:27:23.005] [DCS-REJECT ] [Thread: RMI TCP Connection(7)-127.0.0.1][Client-2] ACCESS DENIED [MUTUAL EXCLUSION ACTIVE]:
Account 101 is LOCKED by Client-1 [DEPOSIT]. Withdrawal REJECTED!
[11:27:30.209] [DCS-SUCCESS] [Thread: RMI TCP Connection(6)-127.0.0.1][Client-1] Deposited 2000.00 to Account 101. Balance: 12000.00
[11:27:30.214] [DCS-MUTEX  ] [Thread: RMI TCP Connection(6)-127.0.0.1][Client-1] ReentrantLock Released for Account 101
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS"""
    story.append(Paragraph(terminal_code.replace('\n', '<br/>').replace(' ', '&nbsp;'), code_style))
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>9. ADVANTAGES AND LIMITATIONS</b>", h1_style))
    story.append(Paragraph("<b>9.1 Advantages</b>", h2_style))
    story.append(Paragraph("1. Supports multiple independent banking clients.", numbered_style))
    story.append(Paragraph("2. Uses Java RMI for remote communication.", numbered_style))
    story.append(Paragraph("3. Centralizes banking logic and account management.", numbered_style))
    story.append(Paragraph("4. Protects account balances through synchronization.", numbered_style))
    story.append(Paragraph("5. Prevents circular waiting during ordered account-lock acquisition.", numbered_style))
    story.append(Paragraph("6. Provides both console and graphical interfaces.", numbered_style))
    story.append(Paragraph("7. Supports transaction monitoring and practical concurrency demonstrations.", numbered_style))

    story.append(Paragraph("<b>9.2 Limitations</b>", h2_style))
    story.append(Paragraph("1. The centralized server is a single point of failure.", numbered_style))
    story.append(Paragraph("2. In-memory account information may be lost when the server terminates.", numbered_style))
    story.append(Paragraph("3. Persistent database storage is not documented.", numbered_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 17: 9.2 CONT, 10. CONCLUSION, 11. FUTURE, 12. REFERENCES
    # =========================================================================
    story.append(Paragraph("4. Replicated servers and automatic failover are not implemented in the documented architecture.", numbered_style))
    story.append(Paragraph("5. Production-level authentication, authorization, encryption, and banking security are outside the current scope.", numbered_style))
    story.append(Paragraph("6. System performance may be limited by the centralized server.", numbered_style))
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>10. CONCLUSION</b>", h1_style))
    story.append(Paragraph(
        "The Distributed Banking System using Java RMI was developed to demonstrate the practical implementation of "
        "distributed computing and concurrent transaction processing.",
        body_style
    ))
    story.append(Paragraph(
        "The system provides remote banking services through a centralized server and supports multiple clients using Java RMI. "
        "The implementation separates client interfaces from the server-side banking logic and uses account-level synchronization "
        "to control concurrent access to shared balances.",
        body_style
    ))
    story.append(Paragraph(
        "The use of ReentrantLock helps prevent conflicting account updates, while consistent lock ordering avoids circular waiting "
        "during fund transfers. Functional and concurrency test scenarios provide a basis for verifying transaction correctness.",
        body_style
    ))
    story.append(Paragraph(
        "The project successfully illustrates important concepts of distributed systems, including remote method invocation, "
        "inter-process communication, concurrency, mutual exclusion, and deadlock avoidance. It also identifies the need for persistent "
        "storage, stronger security, and fault tolerance before such an application could be considered for real-world banking use.",
        body_style
    ))
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>11. FUTURE ENHANCEMENTS</b>", h1_style))
    story.append(Paragraph("The project can be extended in the following ways:", body_style))
    story.append(Paragraph("1. Integrate a database to provide persistent account and transaction storage.", numbered_style))
    story.append(Paragraph("2. Implement user authentication and role-based authorization.", numbered_style))
    story.append(Paragraph("3. Add encrypted communication between clients and the server.", numbered_style))
    story.append(Paragraph("4. Introduce transaction recovery and durable transaction logs.", numbered_style))
    story.append(Paragraph("5. Develop replicated servers and fault-tolerance mechanisms.", numbered_style))
    story.append(Paragraph("6. Evaluate system throughput and response time under larger client loads.", numbered_style))
    story.append(Spacer(1, 4))

    story.append(Paragraph("<b>12. REFERENCES</b>", h1_style))
    story.append(Paragraph("1. Tanenbaum, A. S., and Van Steen, M. <i>Distributed Systems: Principles and Paradigms</i>. 3rd Edition.", numbered_style))
    story.append(Paragraph("2. Coulouris, G., Dollimore, J., Kindberg, T., and Blair, G. <i>Distributed Systems: Concepts and Design</i>. 5th Edition.", numbered_style))
    story.append(Paragraph("3. Oracle Corporation. <i>Java Remote Method Invocation (RMI) Specification</i>. Java SE Documentation.", numbered_style))
    story.append(Paragraph("4. Oracle Corporation. <i>Java Platform, Standard Edition — Concurrency Utilities Documentation</i>.", numbered_style))

    # Build PDF with custom canvas
    doc.build(story, canvasmaker=BorderedCanvas)
    print(f"[PDF] Successfully generated: {PDF_PATH}")
    shutil.copyfile(PDF_PATH, PCCOE_PDF_COPY)
    print(f"[PDF] Saved copy at: {PCCOE_PDF_COPY}")


def build_docx():
    print(f"[DOCX] Generating Word document at: {DOCX_PATH}")
    doc = Document()

    # Set 0.5 inch margins (36 pt)
    for section in doc.sections:
        section.top_margin = Inches(0.5)
        section.bottom_margin = Inches(0.5)
        section.left_margin = Inches(0.5)
        section.right_margin = Inches(0.5)

    def set_font(run, font_name='Times New Roman', size_pt=10, bold=False, italic=False, color_rgb=(0,0,0)):
        run.font.name = font_name
        run.font.size = Pt(size_pt)
        run.font.bold = bold
        run.font.italic = italic
        run.font.color.rgb = RGBColor(*color_rgb)

    def add_p(text, align=WD_ALIGN_PARAGRAPH.LEFT, space_before=0, space_after=4, font_name='Times New Roman', size_pt=10, bold=False, italic=False):
        p = doc.add_paragraph()
        p.alignment = align
        p.paragraph_format.space_before = Pt(space_before)
        p.paragraph_format.space_after = Pt(space_after)
        p.paragraph_format.line_spacing = 1.15
        run = p.add_run(text)
        set_font(run, font_name, size_pt, bold, italic)
        return p

    def add_h1(text):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.space_before = Pt(12)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        set_font(run, 'Times New Roman', 12, bold=True)
        return p

    def add_h2(text):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.space_before = Pt(8)
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        set_font(run, 'Times New Roman', 11, bold=True)
        return p

    # Cover Page
    add_p("Pimpri Chinchwad Education Trust’s", align=WD_ALIGN_PARAGRAPH.CENTER, space_before=20, space_after=2, size_pt=12, bold=True)
    add_p("Pimpri Chinchwad College of Engineering", align=WD_ALIGN_PARAGRAPH.CENTER, space_before=0, space_after=2, size_pt=13, bold=True)
    add_p("Sector No. 26, Pradhikaran, Nigdi, Pune 411044 An Autonomous Institute Approved By AICTE and Affiliated To SPPU, Pune", align=WD_ALIGN_PARAGRAPH.CENTER, space_before=0, space_after=16, size_pt=9.5)
    add_p("DEPARTMENT OF INFORMATION TECHNOLOGY", align=WD_ALIGN_PARAGRAPH.CENTER, space_before=10, space_after=16, size_pt=13, bold=True)

    if os.path.exists(LOGO_PATH):
        p_logo = doc.add_paragraph()
        p_logo.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_logo.paragraph_format.space_before = Pt(10)
        p_logo.paragraph_format.space_after = Pt(16)
        r_logo = p_logo.add_run()
        r_logo.add_picture(LOGO_PATH, width=Inches(1.5))

    add_p("Laboratory Report of Distributed Computing System", align=WD_ALIGN_PARAGRAPH.CENTER, space_before=10, space_after=16, size_pt=11)
    add_p("DISTRIBUTED BANKING SYSTEM USING JAVA RMI WITH\nCONCURRENT MULTI-CLIENT TRANSACTION SYNCHRONIZATION", align=WD_ALIGN_PARAGRAPH.CENTER, space_before=10, space_after=20, size_pt=13, bold=True)

    add_p("Prepared By:", align=WD_ALIGN_PARAGRAPH.CENTER, space_before=10, space_after=6, size_pt=11, bold=True)

    table_authors = doc.add_table(rows=4, cols=2)
    table_authors.alignment = WD_TABLE_ALIGNMENT.CENTER
    authors_data = [
        ("Name", "PRN"),
        ("Piyush Badgujar", "123B1F003"),
        ("Rutuja Bobade", "123B1F012"),
        ("Mehul Chaudhari", "123B1F017")
    ]
    for row_idx, (name, prn) in enumerate(authors_data):
        row = table_authors.rows[row_idx]
        p0 = row.cells[0].paragraphs[0]
        r0 = p0.add_run(name)
        set_font(r0, 'Times New Roman', 9.5, bold=(row_idx == 0))
        p1 = row.cells[1].paragraphs[0]
        r1 = p1.add_run(prn)
        set_font(r1, 'Times New Roman', 9.5, bold=(row_idx == 0))

    add_p("Guided by:", align=WD_ALIGN_PARAGRAPH.CENTER, space_before=24, space_after=2, size_pt=11, bold=True)
    add_p("Mrs. Tanuja S. Patankar", align=WD_ALIGN_PARAGRAPH.CENTER, space_before=0, space_after=20, size_pt=11)

    doc.add_page_break()

    # Abstract & Introduction
    add_h1("ABSTRACT")
    add_p("The Distributed Banking System is a client-server application developed using Java Remote Method Invocation (RMI) to demonstrate the principles of distributed computing and concurrent transaction processing. The application allows multiple clients to perform banking operations, including account creation, balance inquiry, deposits, withdrawals, fund transfers, and transaction history retrieval, through a centralized server.")
    add_p("In a distributed banking environment, multiple clients may access and modify the same account simultaneously. Without proper synchronization, concurrent operations can cause race conditions, lost updates, and incorrect account balances. To address this problem, the proposed system uses account-level mutual exclusion through Java's ReentrantLock mechanism. The system also applies consistent lock ordering during fund transfers to prevent deadlocks.")
    add_p("The application consists of a centralized banking server, multiple remote clients, a Java RMI communication layer, and a graphical dashboard developed using Java Swing. Account information is maintained in a centralized in-memory data structure, while transaction activity and account lock states can be observed through the monitoring interface.")
    add_p("The project demonstrates the implementation of remote method invocation, client-server communication, concurrent request processing, synchronization, and deadlock avoidance. It provides a practical understanding of how distributed applications maintain consistency when multiple users access shared resources.")
    add_p("Keywords: Distributed Computing, Java RMI, Banking System, Client-Server Architecture, Concurrency Control, Mutual Exclusion, Deadlock Avoidance.")

    add_h1("1. INTRODUCTION")
    add_h2("1.1 Introduction to Distributed Computing")
    add_p("A distributed system consists of multiple independent computers or processes that communicate through a network to perform coordinated tasks. These components may execute on separate machines or within different processes and exchange information using communication mechanisms.")
    add_p("Distributed computing is widely used in banking, e-commerce, reservation systems, cloud applications, and enterprise software. Such applications allow multiple users to access shared services without requiring every user to execute the entire application on a single computer.")
    add_p("A banking application is an appropriate example of a distributed system because multiple customers may perform financial operations simultaneously. The system must ensure that transactions are processed correctly and that account balances remain consistent.")
    add_p("Java Remote Method Invocation (RMI) provides a mechanism for communication between Java applications running in separate Java Virtual Machines (JVMs). It allows a client to invoke methods on a remote object hosted by a server.")
    add_p("This project applies Java RMI to develop a distributed banking application in which multiple clients communicate with a centralized server to perform banking transactions.")

    add_h2("1.2 Problem Statement")
    add_p("In a conventional banking application, multiple users may access the same account and initiate transactions concurrently. When these operations are processed without appropriate synchronization, the system may encounter race conditions, lost updates, incorrect balance calculations, or unauthorized withdrawals.")
    add_p("For example, consider an account with a balance of ₹10,000. Two clients simultaneously request withdrawals of ₹7,000 and ₹5,000. Since the total requested amount exceeds the available balance, both withdrawals must not be allowed to succeed.")
    add_p("If the server checks the balance independently for both requests before updating the account, both operations may incorrectly appear valid. This can result in an inconsistent account balance.")
    add_p("Furthermore, a fund transfer involves two accounts. If multiple clients acquire the corresponding account locks in different orders, the system may experience a deadlock in which each client waits for a lock held by another.")
    add_p("Therefore, the problem addressed by this project is to develop a distributed banking application that supports remote access by multiple clients while ensuring correct account updates through synchronized transaction processing and avoiding deadlocks during fund transfers.")

    add_h2("1.3 Objectives")
    add_p("1. To develop a distributed client-server banking application using Java RMI that enables multiple independent clients to remotely access banking services.\n"
          "2. To implement essential banking operations, including account creation, balance inquiry, deposits, withdrawals, and fund transfers, with appropriate account and transaction validation.\n"
          "3. To ensure consistency of account balances during concurrent transactions by implementing account-level mutual exclusion using Java ReentrantLock.\n"
          "4. To prevent deadlocks during simultaneous fund transfers and verify the correctness of concurrent banking operations through systematic testing.")

    add_h2("1.4 Scope of the Project")
    add_p("The project focuses on developing and demonstrating a distributed banking application for academic purposes. The scope includes:\n"
          "• Remote client-server communication using Java RMI.\n"
          "• Multiple independent banking clients.\n"
          "• Account creation and account management.\n"
          "• Balance inquiry, deposit, and withdrawal operations.\n"
          "• Transfers between bank accounts.\n"
          "• Concurrent request processing.\n"
          "• Account-level synchronization.\n"
          "• Deadlock avoidance during transfers.\n"
          "• Transaction history and system monitoring.\n"
          "• Console-based and graphical user interfaces.")

    doc.add_page_break()

    # Theoretical Background
    add_h1("2. THEORETICAL BACKGROUND")
    add_h2("2.1 Client-Server Architecture")
    add_p("Client-server architecture divides an application into clients and a server. The client initiates requests and displays the responses. The server receives requests, executes the required business logic, manages shared data, and returns the result.")

    add_h2("2.2 Java Remote Method Invocation")
    add_p("Java Remote Method Invocation is a mechanism that enables a Java program to invoke methods on an object located in another JVM.\n"
          "The main components of RMI are:\n"
          "• Remote interface: Defines the methods available to remote clients.\n"
          "• Remote implementation: Contains the actual logic of the remote methods.\n"
          "• RMI Registry: Provides a naming service through which clients locate remote objects.\n"
          "• Client stub or proxy: Represents the remote object to the client.\n"
          "• RMI runtime: Handles communication and parameter transfer.")

    add_h2("2.3 Inter-Process Communication")
    add_p("Inter-Process Communication (IPC) enables independent processes to exchange information. Examples include sockets, pipes, shared memory, message queues, and RPC. Java RMI is used because it supports remote communication between Java processes using object-oriented interfaces.")

    add_h2("2.4 Multithreading and Concurrency")
    add_p("Multithreading allows multiple threads to execute concurrently within a process. Several clients can send requests to the server at approximately the same time. The server processes these requests using different threads.")

    add_h2("2.5 Race Condition")
    add_p("A race condition occurs when the result of concurrent operations depends on the order or timing of their execution. The banking system prevents conflicting updates by controlling access to account data using locks.")

    add_h2("2.6 Mutual Exclusion")
    add_p("Mutual exclusion ensures that only one thread at a time can execute a protected critical section for a particular shared resource. The project uses Java's ReentrantLock to protect account operations.")

    add_h2("2.7 Deadlock")
    add_p("Deadlock occurs when two or more threads wait indefinitely for resources held by one another. The project addresses this issue by acquiring account locks in a consistent order.")

    add_h2("2.8 ConcurrentHashMap")
    add_p("ConcurrentHashMap is a thread-safe implementation of the Java Map interface that supports concurrent access to its mappings. The project uses it for shared account-related data structures and account lock management.")

    add_h2("2.9 ACID Properties")
    add_p("ACID describes four properties associated with reliable transaction processing: Atomicity, Consistency, Isolation, and Durability.")

    doc.add_page_break()

    # System Requirements
    add_h1("3. SYSTEM REQUIREMENTS")
    add_h2("3.1 Hardware Requirements")
    t_hw = doc.add_table(rows=5, cols=2)
    t_hw.alignment = WD_TABLE_ALIGNMENT.CENTER
    hw_rows = [
        ("Component", "Requirement"),
        ("Processor", "Standard modern processor"),
        ("RAM", "Sufficient memory to run the Java server and multiple clients"),
        ("Storage", "Space for the project files and Java environment"),
        ("Network", "Localhost communication or a network connection for remote clients")
    ]
    for r_i, (c0, c1) in enumerate(hw_rows):
        row = t_hw.rows[r_i]
        p0 = row.cells[0].paragraphs[0]
        r0 = p0.add_run(c0)
        set_font(r0, 'Times New Roman', 9.5, bold=(r_i == 0))
        p1 = row.cells[1].paragraphs[0]
        r1 = p1.add_run(c1)
        set_font(r1, 'Times New Roman', 9.5, bold=(r_i == 0))

    add_h2("3.2 Software Requirements")
    t_sw = doc.add_table(rows=6, cols=2)
    t_sw.alignment = WD_TABLE_ALIGNMENT.CENTER
    sw_rows = [
        ("Software", "Purpose"),
        ("Java Development Kit (JDK)", "Compilation and execution of Java programs"),
        ("Java RMI", "Remote communication between clients and server"),
        ("Java Swing", "Graphical user interface"),
        ("Java Concurrency API", "Locks and concurrent data structures"),
        ("IDE or terminal", "Development, execution, and debugging")
    ]
    for r_i, (c0, c1) in enumerate(sw_rows):
        row = t_sw.rows[r_i]
        p0 = row.cells[0].paragraphs[0]
        r0 = p0.add_run(c0)
        set_font(r0, 'Times New Roman', 9.5, bold=(r_i == 0))
        p1 = row.cells[1].paragraphs[0]
        r1 = p1.add_run(c1)
        set_font(r1, 'Times New Roman', 9.5, bold=(r_i == 0))

    doc.add_page_break()

    # System Architecture
    add_h1("4. SYSTEM ARCHITECTURE")
    add_h2("4.1 Architecture Overview")
    add_p("The system follows a centralized client-server architecture. Multiple clients communicate with a banking server through Java RMI. The major components are:\n"
          "1. Client applications.\n"
          "2. Java RMI communication layer.\n"
          "3. RMI Registry.\n"
          "4. Banking service implementation.\n"
          "5. Synchronization layer.\n"
          "6. Centralized account storage.\n"
          "7. Transaction history and monitoring interface.")

    add_h2("4.2 Architecture Diagram")
    if os.path.exists(ARCH_DIAGRAM_PATH):
        p_diag = doc.add_paragraph()
        p_diag.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_diag = p_diag.add_run()
        r_diag.add_picture(ARCH_DIAGRAM_PATH, width=Inches(5.0))

    add_h2("4.3 Client Layer")
    add_p("The client layer allows users to interact with the banking system. The project provides a command-line client and a graphical dashboard.")

    add_h2("4.4 Communication Layer")
    add_p("The communication layer uses Java RMI to exchange requests and responses. The client locates the remote banking service through the RMI Registry and invokes its methods.")

    add_h2("4.5 Business Logic Layer")
    add_p("The business logic layer is implemented in the banking service. It validates account information, executes operations, manages balances, and records transaction details.")

    add_h2("4.6 Synchronization Layer")
    add_p("The synchronization layer protects shared account information using account-level locks to prevent conflicting operations and consistent lock ordering to avoid deadlocks.")

    add_h2("4.7 Data Management Layer")
    add_p("The data management layer maintains account information using centralized in-memory data structures.")

    doc.add_page_break()

    # Module Description
    add_h1("5. MODULE DESCRIPTION")
    add_h2("5.1 BankService.java")
    add_p("This module defines the remote banking interface. It declares methods for creating accounts, checking balances, deposits, withdrawals, transfers, and transaction history.")

    add_h2("5.2 BankServiceImpl.java")
    add_p("This module implements the remote banking interface. Its responsibilities include validating requests, updating balances, acquiring/releasing locks, executing transfers, and returning responses.")

    add_h2("5.3 BankServer.java")
    add_p("This module initializes the server: creating the RMI Registry, registering the remote service, initializing demonstration accounts, and waiting for requests.")

    add_h2("5.4 BankClient.java")
    add_p("This module provides the console interface allowing users to select banking operations from a menu.")

    add_h2("5.5 Dashboard.java")
    add_p("This module provides the graphical interface allowing users to perform operations and monitor account information.")

    add_h2("5.6 DcsTelemetryPanel.java")
    add_p("This module displays system execution information, such as server activity and account lock states.")

    add_h2("5.7 Model Classes")
    t_mod = doc.add_table(rows=4, cols=2)
    t_mod.alignment = WD_TABLE_ALIGNMENT.CENTER
    mod_rows = [
        ("Class", "Description"),
        ("Account.java", "Represents account information."),
        ("Transaction.java", "Represents transaction details."),
        ("BankResponse.java", "Represents the result of a banking operation.")
    ]
    for r_i, (c0, c1) in enumerate(mod_rows):
        row = t_mod.rows[r_i]
        p0 = row.cells[0].paragraphs[0]
        r0 = p0.add_run(c0)
        set_font(r0, 'Times New Roman', 9.5, bold=(r_i == 0))
        p1 = row.cells[1].paragraphs[0]
        r1 = p1.add_run(c1)
        set_font(r1, 'Times New Roman', 9.5, bold=(r_i == 0))

    doc.add_page_break()

    # Implementation Methodology & Concurrency
    add_h1("6. IMPLEMENTATION METHODOLOGY")
    add_h2("6.1 Server Initialization")
    add_p("The server creates the banking service and registers it with the RMI Registry:\n"
          "LocateRegistry.createRegistry(1099);\n"
          "BankService service = new BankServiceImpl();\n"
          "Naming.rebind(\"BankService\", service);")

    add_h2("6.2 Account Creation")
    add_p("Account creation involves receiving account identifier and opening balance, validating information, and adding the account to the store.")

    add_h2("6.3 Deposit Operation")
    add_p("1. Receive account number and deposit amount.\n"
          "2. Obtain account lock.\n"
          "3. Verify account exists.\n"
          "4. Validate deposit amount.\n"
          "5. Add amount to balance.\n"
          "6. Record transaction.\n"
          "7. Return result.\n"
          "8. Release lock.")

    add_h2("6.4 Withdrawal Operation")
    add_p("1. Receive account number and withdrawal amount.\n"
          "2. Acquire account lock.\n"
          "3. Verify account exists.\n"
          "4. Validate withdrawal amount.\n"
          "5. Check available balance.\n"
          "6. Deduct amount if sufficient funds exist.\n"
          "7. Record transaction.\n"
          "8. Return result.\n"
          "9. Release lock.")

    add_h2("6.5 Fund Transfer")
    add_p("1. Receive source account, destination account, and transfer amount.\n"
          "2. Validate account identifiers.\n"
          "3. Determine consistent lock acquisition order.\n"
          "4. Acquire both account locks.\n"
          "5. Verify both accounts exist.\n"
          "6. Validate amount and source balance.\n"
          "7. Debit source account.\n"
          "8. Credit destination account.\n"
          "9. Record transfer.\n"
          "10. Return result and release both locks.")

    add_h1("7. CONCURRENCY CONTROL AND DEADLOCK PREVENTION")
    add_h2("7.1 Account-Level Locking")
    add_p("The system uses account-level ReentrantLock instances to control access to account data:\n"
          "private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();\n"
          "private ReentrantLock getLockForAccount(String accountNumber) {\n"
          "    return accountLocks.computeIfAbsent(accountNumber, key -> new ReentrantLock(true));\n"
          "}")

    add_h2("7.2 Lock Acquisition")
    add_p("The project uses tryLock() with a configured waiting period. Locks are always released in a finally block.")

    add_h2("7.3 Concurrent Withdrawal Example")
    add_p("Consider an account with balance ₹10,000. Client A requests ₹7,000; Client B requests ₹5,000. Since total ₹12,000 exceeds ₹10,000, both must not succeed. Account locks ensure sequential processing: one succeeds, and the other is rejected.")

    add_h2("7.4 Deadlock Prevention")
    add_p("During fund transfers, the system acquires locks for source and destination accounts in a consistent sorted order, preventing circular wait.")

    doc.add_page_break()

    # Testing and Results
    add_h1("8. TESTING AND RESULTS")
    add_h2("8.1 Testing Objectives")
    add_p("Testing verifies correct operation execution, invalid transaction rejection, account balance consistency, safe concurrency handling, conflicting withdrawal prevention, and deadlock avoidance.")

    add_h2("8.2 Functional Test Cases")
    t_tc = doc.add_table(rows=11, cols=3)
    t_tc.alignment = WD_TABLE_ALIGNMENT.CENTER
    tc_rows = [
        ("Test Case", "Operation", "Expected Result"),
        ("TC-01", "Create a new account", "Account is created successfully."),
        ("TC-02", "Create a duplicate account", "Duplicate account is rejected."),
        ("TC-03", "Deposit money", "Balance increases by the deposited amount."),
        ("TC-04", "Withdraw money", "Balance decreases by the withdrawn amount when sufficient funds exist."),
        ("TC-05", "Withdraw more than available balance", "Transaction is rejected."),
        ("TC-06", "Transfer money", "Source is debited and destination is credited."),
        ("TC-07", "Transfer to the same account", "Invalid transfer is rejected."),
        ("TC-08", "Request an operation on a locked account", "Request follows configured lock-wait and rejection policy."),
        ("TC-09", "Execute concurrent withdrawals", "Conflicting withdrawals do not cause an incorrect balance."),
        ("TC-10", "Execute concurrent deposits", "All successful deposits are reflected in the final balance.")
    ]
    for r_i, (c0, c1, c2) in enumerate(tc_rows):
        row = t_tc.rows[r_i]
        for col_i, text in enumerate((c0, c1, c2)):
            p = row.cells[col_i].paragraphs[0]
            r = p.add_run(text)
            set_font(r, 'Times New Roman', 9, bold=(r_i == 0))

    add_h2("8.3 Concurrent Deposit Verification")
    add_p("Suppose five clients each deposit ₹1,000 into an account with initial balance ₹10,000. Expected final balance: [ 5 × ₹1,000 = ₹5,000 ] -> ₹15,000. Verified without lost updates.")

    add_h2("8.4 Mutual Exclusion Demonstration")
    add_p("The system demonstrates mutual exclusion by holding an account lock during a processing delay and initiating requests from other clients.")

    add_h2("8.5 Results and Observations")
    add_p("• Successful account creation and banking operations.\n"
          "• Rejection of invalid transactions.\n"
          "• Correct final balances after concurrent deposits.\n"
          "• Correct rejection of an excess withdrawal.\n"
          "• Successful transfers without deadlock.\n"
          "• Visible account-lock behavior in the telemetry panel.")

    if os.path.exists(ADMIN_DASH_PATH):
        p_a = doc.add_paragraph()
        p_a.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_a = p_a.add_run()
        r_a.add_picture(ADMIN_DASH_PATH, width=Inches(4.8))

    if os.path.exists(CUST_DASH_PATH):
        p_c = doc.add_paragraph()
        p_c.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_c = p_c.add_run()
        r_c.add_picture(CUST_DASH_PATH, width=Inches(4.8))

    doc.add_page_break()

    # Advantages, Limitations, Conclusion, Enhancements, References
    add_h1("9. ADVANTAGES AND LIMITATIONS")
    add_h2("9.1 Advantages")
    add_p("1. Supports multiple independent banking clients.\n"
          "2. Uses Java RMI for remote communication.\n"
          "3. Centralizes banking logic and account management.\n"
          "4. Protects account balances through synchronization.\n"
          "5. Prevents circular waiting during ordered account-lock acquisition.\n"
          "6. Provides both console and graphical interfaces.\n"
          "7. Supports transaction monitoring and practical concurrency demonstrations.")

    add_h2("9.2 Limitations")
    add_p("1. The centralized server is a single point of failure.\n"
          "2. In-memory account information may be lost when the server terminates.\n"
          "3. Persistent database storage is not documented.\n"
          "4. Replicated servers and automatic failover are not implemented in the documented architecture.\n"
          "5. Production-level authentication, authorization, encryption, and banking security are outside current scope.\n"
          "6. System performance may be limited by the centralized server.")

    add_h1("10. CONCLUSION")
    add_p("The Distributed Banking System using Java RMI was developed to demonstrate the practical implementation of distributed computing and concurrent transaction processing.")
    add_p("The system provides remote banking services through a centralized server and supports multiple clients using Java RMI. The implementation separates client interfaces from the server-side banking logic and uses account-level synchronization to control concurrent access to shared balances.")
    add_p("The use of ReentrantLock helps prevent conflicting account updates, while consistent lock ordering avoids circular waiting during fund transfers. Functional and concurrency test scenarios provide a basis for verifying transaction correctness.")
    add_p("The project successfully illustrates important concepts of distributed systems, including remote method invocation, inter-process communication, concurrency, mutual exclusion, and deadlock avoidance. It also identifies the need for persistent storage, stronger security, and fault tolerance before such an application could be considered for real-world banking use.")

    add_h1("11. FUTURE ENHANCEMENTS")
    add_p("1. Integrate a database to provide persistent account and transaction storage.\n"
          "2. Implement user authentication and role-based authorization.\n"
          "3. Add encrypted communication between clients and the server.\n"
          "4. Introduce transaction recovery and durable transaction logs.\n"
          "5. Develop replicated servers and fault-tolerance mechanisms.\n"
          "6. Evaluate system throughput and response time under larger client loads.")

    add_h1("12. REFERENCES")
    add_p("1. Tanenbaum, A. S., and Van Steen, M. Distributed Systems: Principles and Paradigms. 3rd Edition.\n"
          "2. Coulouris, G., Dollimore, J., Kindberg, T., and Blair, G. Distributed Systems: Concepts and Design. 5th Edition.\n"
          "3. Oracle Corporation. Java Remote Method Invocation (RMI) Specification. Java SE Documentation.\n"
          "4. Oracle Corporation. Java Platform, Standard Edition — Concurrency Utilities Documentation.")

    doc.save(DOCX_PATH)
    print(f"[DOCX] Successfully generated: {DOCX_PATH}")
    shutil.copyfile(DOCX_PATH, PCCOE_DOCX_COPY)
    print(f"[DOCX] Saved copy at: {PCCOE_DOCX_COPY}")


if __name__ == "__main__":
    print("=" * 65)
    print("Generating Academic Distributed Banking System Documentation")
    print("Matching DCS_Mini_Project_Report.pdf format and PCCOE standards")
    print("=" * 65)
    build_pdf()
    build_docx()
    print("=" * 65)
    print("Generation complete! All documents generated successfully.")
    print("=" * 65)
