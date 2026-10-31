package com.nocapai.gui;

import com.nocapai.ds.MyQueue;
import com.nocapai.model.Book;
import com.nocapai.model.Member;
import com.nocapai.service.LibraryService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Waiting Queue module — shows the FIFO queue of members waiting for a book. */
public class QueuePanel extends JPanel {

    private final LibraryService service;
    private JComboBox<String> bookSelector;
    private DefaultListModel<String> listModel;
    private JList<String> queueList;

    public QueuePanel(LibraryService service) {
        this.service = service;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildUI();
    }

    private void buildUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bookSelector = new JComboBox<>();
        JButton viewButton = new JButton("View Queue");
        JButton refreshListButton = new JButton("Refresh Book List");

        viewButton.addActionListener(e -> showQueue());
        refreshListButton.addActionListener(e -> populateBookSelector());

        top.add(new JLabel("Books with an active waiting queue:"));
        top.add(bookSelector);
        top.add(viewButton);
        top.add(refreshListButton);

        listModel = new DefaultListModel<>();
        queueList = new JList<>(listModel);
        queueList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(queueList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Queue order (front = next to receive the book)"));

        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        populateBookSelector();
    }

    private void populateBookSelector() {
        bookSelector.removeAllItems();
        List<String> bookIds = service.getBooksWithActiveQueues();
        if (bookIds.isEmpty()) {
            bookSelector.addItem("(no active queues)");
        } else {
            for (String id : bookIds) {
                Book book = service.findBookById(id);
                String label = book != null ? id + " - " + book.getTitle() : id;
                bookSelector.addItem(label);
            }
        }
        listModel.clear();
    }

    private void showQueue() {
        String selected = (String) bookSelector.getSelectedItem();
        listModel.clear();
        if (selected == null || selected.startsWith("(no active queues)")) {
            return;
        }
        String bookId = selected.split(" - ")[0];
        MyQueue<Member> queue = service.getQueueForBook(bookId);
        if (queue == null || queue.isEmpty()) {
            listModel.addElement("No one is currently waiting for this book.");
            return;
        }
        int position = 1;
        for (Member m : queue.toList()) {
            listModel.addElement(position + ". " + m.getName() + " (" + m.getId() + ")");
            position++;
        }
    }
}
