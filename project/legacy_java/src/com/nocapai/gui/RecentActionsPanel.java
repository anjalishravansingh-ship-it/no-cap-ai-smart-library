package com.nocapai.gui;

import com.nocapai.model.Transaction;
import com.nocapai.service.LibraryService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Recent Actions module — displays the Stack of transactions, most recent first (LIFO). */
public class RecentActionsPanel extends JPanel {

    private final LibraryService service;
    private DefaultListModel<String> listModel;
    private JList<String> actionsList;

    public RecentActionsPanel(LibraryService service) {
        this.service = service;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildUI();
        refresh();
    }

    private void buildUI() {
        JLabel info = new JLabel("Most recent action is shown first — this list mirrors the top of the Recent Actions Stack.");
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());

        JPanel top = new JPanel(new BorderLayout());
        top.add(info, BorderLayout.CENTER);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(refreshButton);
        top.add(btnPanel, BorderLayout.EAST);

        listModel = new DefaultListModel<>();
        actionsList = new JList<>(listModel);
        actionsList.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(actionsList);

        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void refresh() {
        listModel.clear();
        List<Transaction> actions = service.getRecentActions();
        if (actions.isEmpty()) {
            listModel.addElement("No actions recorded yet.");
            return;
        }
        for (Transaction t : actions) {
            listModel.addElement(t.toString());
        }
    }
}
