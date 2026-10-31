package com.nocapai.gui;

import com.nocapai.service.LibraryService;

import javax.swing.*;
import java.awt.*;

/** Issue/Return module — the core day-to-day transaction screen. */
public class IssueReturnPanel extends JPanel {

    private final LibraryService service;
    private JTextField issueBookField;
    private JTextField issueMemberField;
    private JTextField returnBookField;
    private JTextField returnMemberField;
    private JTextArea logArea;

    public IssueReturnPanel(LibraryService service) {
        this.service = service;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildUI();
    }

    private void buildUI() {
        JPanel forms = new JPanel(new GridLayout(1, 2, 20, 0));

        // ---- Issue box ----
        JPanel issueBox = new JPanel();
        issueBox.setLayout(new BoxLayout(issueBox, BoxLayout.Y_AXIS));
        issueBox.setBorder(BorderFactory.createTitledBorder("Issue a Book"));
        issueBookField = new JTextField();
        issueMemberField = new JTextField();
        JButton issueButton = new JButton("Issue Book");
        issueButton.addActionListener(e -> doIssue());
        issueBox.add(labeled("Book ID:", issueBookField));
        issueBox.add(Box.createRigidArea(new Dimension(0, 8)));
        issueBox.add(labeled("Member ID:", issueMemberField));
        issueBox.add(Box.createRigidArea(new Dimension(0, 12)));
        issueBox.add(issueButton);

        // ---- Return box ----
        JPanel returnBox = new JPanel();
        returnBox.setLayout(new BoxLayout(returnBox, BoxLayout.Y_AXIS));
        returnBox.setBorder(BorderFactory.createTitledBorder("Return a Book"));
        returnBookField = new JTextField();
        returnMemberField = new JTextField();
        JButton returnButton = new JButton("Return Book");
        returnButton.addActionListener(e -> doReturn());
        returnBox.add(labeled("Book ID:", returnBookField));
        returnBox.add(Box.createRigidArea(new Dimension(0, 8)));
        returnBox.add(labeled("Member ID:", returnMemberField));
        returnBox.add(Box.createRigidArea(new Dimension(0, 12)));
        returnBox.add(returnButton);

        forms.add(issueBox);
        forms.add(returnBox);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Activity Log"));

        add(forms, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);
    }

    private JPanel labeled(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return row;
    }

    private void doIssue() {
        String bookId = issueBookField.getText().trim();
        String memberId = issueMemberField.getText().trim();
        if (bookId.isEmpty() || memberId.isEmpty()) {
            appendLog("Please enter both a Book ID and a Member ID to issue.");
            return;
        }
        String result = service.issueBook(bookId, memberId);
        appendLog(result);
        issueBookField.setText("");
        issueMemberField.setText("");
    }

    private void doReturn() {
        String bookId = returnBookField.getText().trim();
        String memberId = returnMemberField.getText().trim();
        if (bookId.isEmpty() || memberId.isEmpty()) {
            appendLog("Please enter both a Book ID and a Member ID to return.");
            return;
        }
        String result = service.returnBook(bookId, memberId);
        appendLog(result);
        returnBookField.setText("");
        returnMemberField.setText("");
    }

    private void appendLog(String text) {
        logArea.append(text + "\n" + "-".repeat(70) + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
