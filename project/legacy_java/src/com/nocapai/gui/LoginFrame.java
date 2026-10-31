package com.nocapai.gui;

import com.nocapai.model.Member;
import com.nocapai.service.LibraryService;

import javax.swing.*;
import java.awt.*;

/**
 * Login screen. Two roles are supported:
 *   - Librarian: username "admin", password "admin123" -> full access.
 *   - Student:   their Member ID (e.g. "M001") as username,
 *                password "student123" -> restricted, self-service access.
 *
 * This is intentionally simple (no external auth server) since the focus
 * of the project is the data-structure-driven backend, not security.
 */
public class LoginFrame extends JFrame {

    private final LibraryService service;
    private JComboBox<String> roleBox;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame(LibraryService service) {
        this.service = service;
        setTitle("No Cap AI - Smart Library Management | Login");
        setSize(420, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel title = new JLabel("No Cap AI");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Smart Library Management System");
        subtitle.setFont(new Font("SansSerif", Font.ITALIC, 13));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(new Color(90, 90, 90));

        roleBox = new JComboBox<>(new String[]{"Librarian", "Student"});
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> attemptLogin());

        JLabel hint = new JLabel("<html><i>Librarian: admin / admin123<br>Student: your Member ID / student123</i></html>");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(new Color(120, 120, 120));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(subtitle);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(formRow("Role:", roleBox));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(formRow("Username / ID:", usernameField));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(formRow("Password:", passwordField));
        panel.add(Box.createRigidArea(new Dimension(0, 16)));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(loginButton);
        panel.add(Box.createRigidArea(new Dimension(0, 14)));
        panel.add(hint);

        setContentPane(panel);
        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel formRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(320, 30));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(110, 20));
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private void attemptLogin() {
        String role = (String) roleBox.getSelectedItem();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.",
                    "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if ("Librarian".equals(role)) {
            if (username.equals("admin") && password.equals("admin123")) {
                openDashboard("Librarian", null);
            } else {
                showError("Invalid librarian credentials.");
            }
        } else {
            Member member = service.findMemberById(username);
            if (member == null) {
                showError("No student/member found with ID \"" + username + "\".");
                return;
            }
            if (!password.equals("student123")) {
                showError("Invalid password.");
                return;
            }
            openDashboard("Student", member);
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Login failed", JOptionPane.ERROR_MESSAGE);
    }

    private void openDashboard(String role, Member member) {
        MainDashboard dashboard = new MainDashboard(service, role, member);
        dashboard.setVisible(true);
        this.dispose();
    }
}
