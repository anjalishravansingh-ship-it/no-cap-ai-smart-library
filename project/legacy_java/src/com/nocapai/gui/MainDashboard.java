package com.nocapai.gui;

import com.nocapai.model.Member;
import com.nocapai.service.LibraryService;

import javax.swing.*;
import java.awt.*;

/**
 * The main window shown after login. Shows a different set of tabs
 * depending on whether the user logged in as Librarian or Student.
 */
public class MainDashboard extends JFrame {

    private final LibraryService service;
    private final String role;
    private final Member currentMember; // null when role == Librarian

    public MainDashboard(LibraryService service, String role, Member currentMember) {
        this.service = service;
        this.role = role;
        this.currentMember = currentMember;

        String who = "Librarian".equals(role) ? "Librarian (admin)" : currentMember.getName();
        setTitle("No Cap AI - Smart Library Management | " + who);
        setSize(950, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        header.setBackground(new Color(31, 56, 100));
        JLabel titleLabel = new JLabel("No Cap AI — Smart Library Management");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(titleLabel, BorderLayout.WEST);

        String who = "Librarian".equals(role) ? "Logged in as: Librarian" : "Logged in as: " + currentMember.getName() + " (" + currentMember.getId() + ")";
        JLabel whoLabel = new JLabel(who);
        whoLabel.setForeground(Color.WHITE);
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightHeader.setOpaque(false);
        rightHeader.add(whoLabel);
        rightHeader.add(logoutButton);
        header.add(rightHeader, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();

        if ("Librarian".equals(role)) {
            tabs.addTab("Books", new BookPanel(service, true, null));
            tabs.addTab("Members", new MemberPanel(service));
            tabs.addTab("Issue / Return", new IssueReturnPanel(service));
            tabs.addTab("Waiting Queue", new QueuePanel(service));
            tabs.addTab("Recent Actions", new RecentActionsPanel(service));
            tabs.addTab("Reports", new ReportsPanel(service));
        } else {
            tabs.addTab("Search Books", new BookPanel(service, false, currentMember));
            tabs.addTab("My Books", new MyBooksPanel(service, currentMember));
            tabs.addTab("Waiting Queue", new QueuePanel(service));
        }

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void logout() {
        LoginFrame login = new LoginFrame(service);
        login.setVisible(true);
        this.dispose();
    }
}
