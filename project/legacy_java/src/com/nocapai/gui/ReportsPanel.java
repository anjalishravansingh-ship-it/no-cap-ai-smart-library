package com.nocapai.gui;

import com.nocapai.model.Book;
import com.nocapai.service.LibraryService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/** Reports module — currently issued, overdue, most popular, and active queues. */
public class ReportsPanel extends JPanel {

    private final LibraryService service;
    private JTextArea outputArea;

    public ReportsPanel(LibraryService service) {
        this.service = service;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildUI();
    }

    private void buildUI() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton issuedButton = new JButton("Currently Issued Books");
        JButton overdueButton = new JButton("Overdue Books");
        JButton popularButton = new JButton("Most Popular Books");
        JButton queuesButton = new JButton("Active Waiting Queues");

        issuedButton.addActionListener(e -> showCurrentlyIssued());
        overdueButton.addActionListener(e -> showOverdue());
        popularButton.addActionListener(e -> showPopular());
        queuesButton.addActionListener(e -> showActiveQueues());

        buttonPanel.add(issuedButton);
        buttonPanel.add(overdueButton);
        buttonPanel.add(popularButton);
        buttonPanel.add(queuesButton);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(outputArea);

        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void showCurrentlyIssued() {
        List<LibraryService.IssuedRecord> records = service.getCurrentlyIssued();
        StringBuilder sb = new StringBuilder("CURRENTLY ISSUED BOOKS (" + records.size() + ")\n");
        sb.append("=".repeat(60)).append("\n");
        for (LibraryService.IssuedRecord r : records) {
            sb.append(String.format("%-30s -> %-20s (due %s)%n",
                    r.book.getTitle(), r.member.getName(), r.dueDate));
        }
        if (records.isEmpty()) sb.append("No books are currently issued.\n");
        outputArea.setText(sb.toString());
    }

    private void showOverdue() {
        List<LibraryService.IssuedRecord> records = service.getOverdueBooks();
        StringBuilder sb = new StringBuilder("OVERDUE BOOKS (" + records.size() + ")\n");
        sb.append("=".repeat(60)).append("\n");
        for (LibraryService.IssuedRecord r : records) {
            sb.append(String.format("%-30s -> %-20s (was due %s)%n",
                    r.book.getTitle(), r.member.getName(), r.dueDate));
        }
        if (records.isEmpty()) sb.append("No overdue books. Everything is on time.\n");
        outputArea.setText(sb.toString());
    }

    private void showPopular() {
        List<Map.Entry<Book, Integer>> entries = service.getMostPopularBooks();
        StringBuilder sb = new StringBuilder("MOST POPULAR BOOKS (by total times issued)\n");
        sb.append("=".repeat(60)).append("\n");
        int rank = 1;
        for (Map.Entry<Book, Integer> entry : entries) {
            sb.append(rank).append(". ").append(entry.getKey().getTitle())
                    .append(" — issued ").append(entry.getValue()).append(" time(s)\n");
            rank++;
        }
        if (entries.isEmpty()) sb.append("No books have been issued yet.\n");
        outputArea.setText(sb.toString());
    }

    private void showActiveQueues() {
        List<String> bookIds = service.getBooksWithActiveQueues();
        StringBuilder sb = new StringBuilder("ACTIVE WAITING QUEUES (" + bookIds.size() + ")\n");
        sb.append("=".repeat(60)).append("\n");
        for (String id : bookIds) {
            Book book = service.findBookById(id);
            int queueSize = service.getQueueForBook(id).size();
            sb.append(book != null ? book.getTitle() : id)
                    .append(" — ").append(queueSize).append(" member(s) waiting\n");
        }
        if (bookIds.isEmpty()) sb.append("No books currently have anyone waiting.\n");
        outputArea.setText(sb.toString());
    }
}
