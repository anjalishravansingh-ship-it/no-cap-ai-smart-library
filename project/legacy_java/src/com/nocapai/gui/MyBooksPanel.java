package com.nocapai.gui;

import com.nocapai.model.Member;
import com.nocapai.service.LibraryService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/** Student self-service screen: view their own issued books, due dates, and overdue status. */
public class MyBooksPanel extends JPanel {

    private final LibraryService service;
    private final Member member;
    private DefaultTableModel tableModel;

    public MyBooksPanel(LibraryService service, Member member) {
        this.service = service;
        this.member = member;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildUI();
        refresh();
    }

    private void buildUI() {
        String[] columns = {"Book ID", "Title", "Due Date", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(24);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.add(refreshButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void refresh() {
        tableModel.setRowCount(0);
        LocalDate today = LocalDate.now();
        for (String bookId : member.getIssuedDueDates().keys()) {
            LocalDate due = member.getIssuedDueDates().get(bookId);
            var book = service.findBookById(bookId);
            String title = book != null ? book.getTitle() : bookId;
            String status = today.isAfter(due) ? "OVERDUE" : "On time";
            tableModel.addRow(new Object[]{bookId, title, due, status});
        }
        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{"-", "You have no books issued right now.", "-", "-"});
        }
    }
}
