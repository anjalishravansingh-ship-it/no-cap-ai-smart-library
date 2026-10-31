package com.nocapai.gui;

import com.nocapai.model.Book;
import com.nocapai.model.Member;
import com.nocapai.service.LibraryService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Book Management module (librarian: full CRUD) and Search & Sort module
 * (student: browse/search + request/issue a book).
 */
public class BookPanel extends JPanel {

    private final LibraryService service;
    private final boolean isLibrarian;
    private final Member currentMember; // used only when !isLibrarian

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;

    public BookPanel(LibraryService service, boolean isLibrarian, Member currentMember) {
        this.service = service;
        this.isLibrarian = isLibrarian;
        this.currentMember = currentMember;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        buildUI();
        refreshTable(service.getAllBooks());
    }

    private void buildUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        searchField = new JTextField(18);
        JButton searchButton = new JButton("Search");
        JButton sortButton = new JButton("Sort by Title (Merge Sort)");
        JButton refreshButton = new JButton("Show All");

        searchButton.addActionListener(e -> refreshTable(service.searchBooks(searchField.getText())));
        sortButton.addActionListener(e -> refreshTable(service.getBooksSortedByTitle()));
        refreshButton.addActionListener(e -> refreshTable(service.getAllBooks()));

        top.add(new JLabel("Search (ID / Title / Author):"));
        top.add(searchField);
        top.add(searchButton);
        top.add(sortButton);
        top.add(refreshButton);

        String[] columns = {"ID", "Title", "Author", "Genre", "Available", "Total"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        if (isLibrarian) {
            JButton addButton = new JButton("Add Book");
            JButton editButton = new JButton("Edit Selected");
            JButton deleteButton = new JButton("Delete Selected");
            addButton.addActionListener(e -> showAddDialog());
            editButton.addActionListener(e -> showEditDialog());
            deleteButton.addActionListener(e -> deleteSelected());
            bottom.add(addButton);
            bottom.add(editButton);
            bottom.add(deleteButton);
        } else {
            JButton requestButton = new JButton("Issue / Join Queue for Selected Book");
            requestButton.addActionListener(e -> requestSelectedBook());
            bottom.add(requestButton);
        }

        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void refreshTable(List<Book> books) {
        tableModel.setRowCount(0);
        for (Book b : books) {
            tableModel.addRow(new Object[]{
                    b.getId(), b.getTitle(), b.getAuthor(), b.getGenre(),
                    b.getAvailableCopies(), b.getTotalCopies()
            });
        }
    }

    private String getSelectedBookId() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book from the table first.",
                    "No book selected", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (String) tableModel.getValueAt(row, 0);
    }

    private void showAddDialog() {
        JTextField idField = new JTextField();
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JTextField genreField = new JTextField();
        JTextField copiesField = new JTextField();

        JPanel form = buildForm(
                new String[]{"Book ID:", "Title:", "Author:", "Genre:", "Total Copies:"},
                new JComponent[]{idField, titleField, authorField, genreField, copiesField});

        int result = JOptionPane.showConfirmDialog(this, form, "Add New Book",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int copies = Integer.parseInt(copiesField.getText().trim());
                String message = service.addBook(idField.getText().trim(), titleField.getText().trim(),
                        authorField.getText().trim(), genreField.getText().trim(), copies);
                JOptionPane.showMessageDialog(this, message);
                refreshTable(service.getAllBooks());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Total Copies must be a whole number.",
                        "Invalid input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditDialog() {
        String id = getSelectedBookId();
        if (id == null) return;
        Book book = service.findBookById(id);
        if (book == null) return;

        JTextField titleField = new JTextField(book.getTitle());
        JTextField authorField = new JTextField(book.getAuthor());
        JTextField genreField = new JTextField(book.getGenre());
        JTextField copiesField = new JTextField(String.valueOf(book.getTotalCopies()));

        JPanel form = buildForm(
                new String[]{"Title:", "Author:", "Genre:", "Total Copies:"},
                new JComponent[]{titleField, authorField, genreField, copiesField});

        int result = JOptionPane.showConfirmDialog(this, form, "Edit Book: " + id,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int copies = Integer.parseInt(copiesField.getText().trim());
                String message = service.editBook(id, titleField.getText().trim(),
                        authorField.getText().trim(), genreField.getText().trim(), copies);
                JOptionPane.showMessageDialog(this, message);
                refreshTable(service.getAllBooks());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Total Copies must be a whole number.",
                        "Invalid input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelected() {
        String id = getSelectedBookId();
        if (id == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Delete book " + id + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String message = service.deleteBook(id);
            JOptionPane.showMessageDialog(this, message);
            refreshTable(service.getAllBooks());
        }
    }

    private void requestSelectedBook() {
        String id = getSelectedBookId();
        if (id == null) return;
        String message = service.issueBook(id, currentMember.getId());
        JOptionPane.showMessageDialog(this, message);
        refreshTable(service.getAllBooks());
    }

    private JPanel buildForm(String[] labels, JComponent[] fields) {
        JPanel form = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) {
            form.add(new JLabel(labels[i]));
            form.add(fields[i]);
        }
        return form;
    }
}
